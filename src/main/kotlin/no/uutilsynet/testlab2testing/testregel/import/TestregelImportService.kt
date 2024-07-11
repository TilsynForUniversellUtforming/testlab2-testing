package no.uutilsynet.testlab2testing.testregel.import

import GithubFolder
import TestregelMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.Charset
import java.time.Instant
import java.util.Base64
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

// import kotlin.io.encoding.Base64

private const val TESTREGLAR = "Testreglar"

@Service
class TestregelImportService(
    val restTemplate: RestTemplate,
    val properties: GithubProperties,
    val kravregisterClient: KravregisterClient,
    val testregelDAO: TestregelDAO
) {

  val repoApiAddress =
      "https://api.github.com/repos/TilsynForUniversellUtforming/testreglar-wcag-2.x/contents/"

  val restClient = RestClient.builder(restTemplate).build()

  val sql =
      """insert into 
            testregel (
              krav_id,
              testregel_schema,
              namn,
              modus,
              testregel_id,
              versjon,
              status,
              dato_sist_endra,
              spraak,
              tema,
              type,
              testobjekt,
              krav_til_samsvar,
              innhaldstype_testing
            ) values (
              :krav_id,
              :testregel_schema,
              :namn,
              :modus,
              :testregel_id,
              :versjon,
              :status,
              :dato_sist_endra,
              :spraak,
              :tema,
              :type,
              :testobjekt,
              :krav_til_samsvar,
              :innhaldstype_testing
            ) 
            returning id
    """
          .trimIndent()

  private val unntakNett = listOf("1.3.4")
  private val unntakApp =
      listOf(
          "1.4.12",
          "1.4.13",
          "2.1.4",
          "2.4.1",
          "2.4.2",
          "2.4.3",
          "2.4.5",
          "2.4.7",
          "3.1.2",
          "3.2.1",
          "3.2.3",
          "3.2.4",
          "4.1.1",
          "4.1.3")

  fun readFolder() {

    val url = repoApiAddress + TESTREGLAR

    println(url)
    println("Token " + properties.token)

    val testregelFolders = doRequest(repoApiAddress + TESTREGLAR)

    println(testregelFolders)
  }

  fun getTestregelList(): List<String> {
    val testregelFolders = doRequest(repoApiAddress + TESTREGLAR)
    val testregelar: List<String>? =
        testregelFolders?.filter { it.name != "felles" }?.map { testregel -> testregel.name }
    return testregelar ?: emptyList()
  }

  fun doRequest(url: String): List<GithubFolder>? {

    val folderResponse =
        restClient
            .get()
            .uri(url)
            .accept(MediaType.parseMediaType("application/vnd.github.raw+json; charset=utf-8"))
            .retrieve()
            .body(Array<GithubFolder>::class.java)

    return folderResponse?.asList()
  }

  fun getTestreglarFolder(): List<GithubFolder>? {
    return doRequest(repoApiAddress + TESTREGLAR)
  }

  fun getTestregelTypeFolder(testregel: String): List<GithubFolder>? {
    return doRequest("$repoApiAddress$TESTREGLAR/$testregel")
  }

  fun getTypeForTestregel(testregel: String, type: TestregelType): List<GithubFolder>? {
    return doRequest("$repoApiAddress$TESTREGLAR/$testregel/$type")
  }

  fun getTestregel(testregel: String, type: TestregelType, name: String): GithubFolder? {
    val url = "$repoApiAddress$TESTREGLAR/$testregel/$type/$name"

    return restClient
        .get()
        .uri(url)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(GithubFolder::class.java)
  }

  fun getTestregelDataAsString(testregel: GithubFolder): String? {
    testregel.content?.let {
      val content =
          Base64.getDecoder().decode(it.replace("\n", "").toByteArray(Charset.forName("UTF-8")))
      val jsonString = String(content, Charset.defaultCharset())
      return jsonString
    }
        ?: return null
  }

  fun githubContentToTestregel(githubSource: String): TestregelInit {
    val objectMapper = ObjectMapper()

    val testregelMeta = objectMapper.readValue(githubSource, TestregelMetadata::class.java)

    val krav = extractKrav(testregelMeta.id)

    val kravId = kravregisterClient.getKrav(krav).getOrThrow().id

    return TestregelInit(
        testregelId = testregelMeta.id,
        namn = testregelMeta.namn,
        kravId = kravId,
        status = TestregelStatus.publisert,
        type = TestregelInnholdstype.valueOf(testregelMeta.type.lowercase()),
        modus = TestregelModus.manuell,
        spraak = TestlabLocale.nn,
        tema = 1,
        testobjekt = 1,
        kravTilSamsvar = testregelMeta.kravTilSamsvar,
        testregelSchema = githubSource,
        innhaldstypeTesting = 1)
  }

  fun extractKrav(testregelId: String): String {
    val kravInit = testregelId.dropLast(1)
    if (kravInit.startsWith("nett")) {
      return kravInit.drop(5)
    }
    if (kravInit.startsWith("app")) {
      return kravInit.drop(4)
    }
    return kravInit
  }

  fun getTestregelFiler() {
    val testregelList = getTestregelList()
  }

  fun getTestreglarApp(testregelList: List<String>): List<String> {
    return testregelList
        .filter { !unntakApp.contains(it) }
        .map { getTypeForTestregel(it, TestregelType.App) }
        .filterNotNull()
        .map { it.map { it.name } }
        .flatten()
  }

  fun getTestreglarNett(testregelList: List<String>): List<String> {
    return testregelList
        .filter { !unntakNett.contains(it) }
        .map { getTypeForTestregel(it, TestregelType.Nett) }
        .filterNotNull()
        .map { it.map { it.name } }
        .flatten()
  }

  fun createOrUpdate(testregel: TestregelInit): Int {
    val existing = testregelDAO.getTestregelByTestregelId(testregel.testregelId)
    return if (existing != null) {
      val updated =
          Testregel(
              id = existing.id,
              testregelId = testregel.testregelId,
              versjon = existing.versjon,
              namn = testregel.namn,
              kravId = testregel.kravId,
              status = TestregelStatus.publisert,
              type = testregel.type,
              testregelSchema = testregel.testregelSchema,
              modus = TestregelModus.manuell,
              spraak = TestlabLocale.nn,
              tema = existing.tema,
              testobjekt = existing.testobjekt,
              kravTilSamsvar = testregel.kravTilSamsvar,
              innhaldstypeTesting = existing.innhaldstypeTesting,
              datoSistEndra = Instant.now(),
          )

      testregelDAO.updateTestregel(updated)
    } else {
      testregelDAO.createTestregel(testregel)
    }
  }
}

enum class TestregelType {
  App,
  Nett
}

@ConfigurationProperties(prefix = "github") data class GithubProperties(val token: String)
