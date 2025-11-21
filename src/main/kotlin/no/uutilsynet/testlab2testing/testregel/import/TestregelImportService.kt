package no.uutilsynet.testlab2testing.testregel.import

import GithubFolder
import TestregelMetadata
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.Charset
import java.time.Instant
import java.util.*
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.*
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelInit
import org.slf4j.LoggerFactory
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

  val logger = LoggerFactory.getLogger(TestregelImportService::class.java)

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
        testregelFolders.filter { it.name != "felles" }.map { testregel -> testregel.name }
    return testregelar ?: emptyList()
  }

  fun doRequest(url: String): List<GithubFolder> {

    val folderResponse =
        restClient
            .get()
            .uri(url)
            .header("Authorization", "token ${properties.token}")
            .accept(MediaType.parseMediaType("application/vnd.github.raw+json; charset=utf-8"))
            .retrieve()
            .body(Array<GithubFolder>::class.java)

    if (folderResponse != null) {
      return folderResponse.asList()
    }
    println("Url $url")
    throw IllegalStateException("No response from github")
  }

  fun getTestreglarFolder(): List<GithubFolder>? = doRequest(repoApiAddress + TESTREGLAR)

  fun getTestregelTypeFolder(testregel: String): List<GithubFolder> =
      doRequest("$repoApiAddress$TESTREGLAR/$testregel")

  fun getTypeForTestregel(testregel: String, type: TestregelType): List<GithubFolder> {
    return doRequest("$repoApiAddress$TESTREGLAR/$testregel/$type")
  }

  fun getTestregel(testregel: String, type: TestregelType, name: String): GithubFolder {
    val url = "$repoApiAddress$TESTREGLAR/$testregel/$type/$name"

    val response =
        restClient
            .get()
            .uri(url)
            .header("Authorization", "token ${properties.token}")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(GithubFolder::class.java)

    if (response != null) {
      return response
    }
    throw IllegalStateException("No response from github")
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
    val testregelMeta =
        runCatching { objectMapper.readValue(githubSource, TestregelMetadata::class.java) }
            .fold(
                onSuccess = { it },
                onFailure = {
                  println("Feil ved mapping av testregel")
                  println(githubSource)
                  throw it
                })

    val krav = extractKrav(testregelMeta.id)

    val kravId = kravregisterClient.getKrav(krav).id

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
    return testregelId.removeSuffix("-2022").dropLast(1).removePrefix("nett-").removePrefix("app-")
  }

  fun getTestreglarApp(testregelList: List<String>): List<String> {
    return testregelList
        .filter { !unntakApp.contains(it) }
        .map { getTypeForTestregel(it, TestregelType.App) }
        .map { it.map { it.name } }
        .flatten()
  }

  fun getTestreglarNett(testregelList: List<String>): List<String> {
    return testregelList
        .filter { !unntakNett.contains(it) }
        .map { getTypeForTestregel(it, TestregelType.Nett) }
        .map { it.map { it.name } }
        .flatten()
  }

  fun getTestreglarForKrav(krav: String, testregelType: TestregelType): List<Int> {
    return getTestregelFiler(krav, testregelType)
        .asSequence()
        .map { getTestregel(krav, testregelType, it) }
        .mapNotNull { getTestregelDataAsString(it) }
        .filter { it.isNotEmpty() }
        .map { githubContentToTestregel(it) }
        .map { createOrUpdate(it) }
        .toList()
  }

  private fun getTestregelFiler(krav: String, testregelType: TestregelType) =
      getTypeForTestregel(krav, testregelType).map { it.name }

  fun createOrUpdate(testregel: TestregelInit): Int {
    val existing = testregelDAO.getTestregelByTestregelId(testregel.testregelId)
    return if (existing != null) {
      logger.info("Update testregel ${testregel.testregelId}")
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
      logger.info("Create testregel ${testregel.testregelId}")
      testregelDAO.createTestregel(testregel)
    }
  }

  fun importTestreglarNett(): Result<Int> {
    return runCatching {
      val testregelList = getTestregelList()
      testregelList
          .filter { !unntakNett.contains(it) }
          .map { getTestreglarForKrav(it, TestregelType.Nett) }
          .flatten()
          .sum()
    }
  }

  fun importTestreglarApp(): Result<Int> {
    return runCatching {
      val testregelList = getTestregelList()
      testregelList
          .filter { !unntakApp.contains(it) }
          .map { getTestreglarForKrav(it, TestregelType.App) }
          .flatten()
          .sum()
    }
  }
}

enum class TestregelType {
  App,
  Nett
}

@ConfigurationProperties(prefix = "github") data class GithubProperties(val token: String)
