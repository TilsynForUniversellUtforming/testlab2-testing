package no.uutilsynet.testlab2testing.testregel.import

import GithubFolder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.Serializers.Base
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import java.nio.charset.Charset
import java.util.Base64

//import kotlin.io.encoding.Base64


private const val TESTREGLAR = "Testreglar"

@Service
class TestregelImportService(val restTemplate: RestTemplate,val properties: GithubProperties) {

  val repoApiAddress =
      "https://api.github.com/repos/TilsynForUniversellUtforming/testreglar-wcag-2.x/contents/"

  val restClient = RestClient.builder(restTemplate).build()

    val sql = """insert into 
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
    """.trimIndent()



  fun readFolder() {

      val url = repoApiAddress + TESTREGLAR

      println(url)
      println("Token " + properties.token)


      val testregelFolders = doRequest(repoApiAddress + TESTREGLAR)


      println(testregelFolders)
  }

    fun getTestregelList(): List<String> {
        val testregelFolders = doRequest(repoApiAddress + TESTREGLAR)
        val testregelar: List<String>? = testregelFolders?.filter { it.name!="felles"}?.map { testregel -> testregel.name }
        return testregelar ?: emptyList()
    }

    fun doRequest(url:String): List<GithubFolder>? {


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

    fun getTestregelTypeFolder(testregel:String): List<GithubFolder>? {
        return doRequest("$repoApiAddress$TESTREGLAR/$testregel")
    }

    fun getTypeForTestregel(testregel:String, type:TestregelType): List<GithubFolder>? {
        return doRequest("$repoApiAddress$TESTREGLAR/$testregel/$type")
    }

    fun getTestregel(testregel:String, type:TestregelType, name:String): GithubFolder? {
        val url = "$repoApiAddress$TESTREGLAR/$testregel/$type/$name"

        return  restClient
            .get()
            .uri(url)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(GithubFolder::class.java)
    }

    fun getTestregelDataAsString(testregel:GithubFolder): String? {
        testregel.content?.let {
            val content = Base64.getDecoder().decode(it.replace("\n","").toByteArray(Charset.forName("UTF-8")))
            val jsonString = String(content, Charset.defaultCharset())
            return jsonString
        } ?: return null
    }



}

enum class TestregelType {
    App,
    Nett
}

@ConfigurationProperties(prefix = "github")
data class GithubProperties(val token: String)
