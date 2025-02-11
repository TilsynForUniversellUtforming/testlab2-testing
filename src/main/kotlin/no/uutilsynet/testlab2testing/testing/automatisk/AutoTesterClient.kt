package no.uutilsynet.testlab2testing.testing.automatisk

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import java.net.URL
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatSide
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatSideTestregel
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatSuksesskriterium
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.Testregel
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "autotester")
data class AutoTesterProperties(val url: String, val code: String)

@Component
class AutoTesterClient(
    val restTemplate: RestTemplate,
    val autoTesterProperties: AutoTesterProperties
) {

  val logger = LoggerFactory.getLogger(AutoTesterClient::class.java)

  fun startTesting(
      maalingId: Int,
      actRegler: List<Testregel>,
      nettsider: List<URL>,
      loeysing: Loeysing,
  ): Result<AutotestingStatus> {

    return runCatching {
          val url = "${autoTesterProperties.url}?code=${autoTesterProperties.code}"
          val requestData =
              mapOf(
                  "urls" to nettsider,
                  "idMaaling" to maalingId,
                  "idLoeysing" to loeysing.id,
                  "resultatSomFil" to true,
                  "actRegler" to actRegler.map { it.testregelSchema },
                  "loeysing" to loeysing)

          val restClient = RestClient.builder(restTemplate).build()

          val statusUris =
              restClient
                  .post()
                  .uri(url)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(requestData)
                  .retrieve()
                  .onStatus(HttpStatusCode::isError) { _, response ->
                    logger.error(response.body.readAllBytes().contentToString())
                    throw RuntimeException("mangler statusQueryGetUri i responsen")
                  }
                  .body(StatusUris::class.java)

          statusUris?.let {
            AutotestingStatus(loeysing, it.statusQueryGetUri.toURL(), nettsider.size)
          }
              ?: throw RuntimeException("mangler statusQueryGetUri i responsen")
        }
        .onFailure {
          logger.error(
              "Kunne ikkje starte test for måling id $maalingId løysing id ${loeysing.id}", it)
        }
  }

  fun updateStatus(testKoeyring: TestKoeyring.Starta): Result<AutoTesterStatus> = runCatching {
    fetchAutoTesterStatus(testKoeyring.statusURL.toURI())!!
  }

  fun updateStatus(testKoeyring: TestKoeyring.IkkjeStarta): Result<AutoTesterStatus> = runCatching {
    fetchAutoTesterStatus(testKoeyring.statusURL.toURI())!!
  }

  private fun fetchAutoTesterStatus(uri: URI) =
      restTemplate.getForObject(uri, AutoTesterStatus::class.java)

  suspend fun fetchResultat(
      testKoeyringar: List<TestKoeyring.Ferdig>,
      resultatType: ResultatUrls,
  ): Map<TestKoeyring.Ferdig, Result<List<AutotesterTestresultat>>> = coroutineScope {
    val deferreds =
        testKoeyringar.map { testKoeyring ->
          async { testKoeyring to fetchResultat(testKoeyring, resultatType) }
        }
    deferreds.awaitAll().toMap()
  }

  private fun fetchResultat(
      testKoeyring: TestKoeyring.Ferdig,
      resultatType: ResultatUrls
  ): Result<List<AutotesterTestresultat>> =
      testKoeyring.lenker?.let { lenker ->
        runCatching {
          when (resultatType) {
            ResultatUrls.urlAggreggeringTR ->
                fetchResultatAggregering(lenker.urlAggregeringTR.toURI(), resultatType)
            ResultatUrls.urlAggregeringSK ->
                fetchResultatAggregering(lenker.urlAggregeringSK.toURI(), resultatType)
            ResultatUrls.urlAggregeringSide ->
                fetchResultatAggregering(lenker.urlAggregeringSide.toURI(), resultatType)
            ResultatUrls.urlAggregeringSideTR ->
                fetchResultatAggregering(lenker.urlAggregeringSideTR.toURI(), resultatType)
            ResultatUrls.urlAggregeringLoeysing ->
                fetchResultatAggregering(lenker.urlAggregeringLoeysing.toURI(), resultatType)
            ResultatUrls.urlFulltResultat -> fetchResultatDetaljert(lenker.urlFulltResultat.toURI())
            ResultatUrls.urlBrot -> fetchResultatDetaljert(lenker.urlBrot.toURI())
          }
        }
      }
          ?: throw IllegalStateException("manglar lenker til testresultat")

  private fun fetchResultatDetaljert(uri: URI): List<TestResultat> {
    return restTemplate
        .getForObject(uri, Array<Array<TestResultat>>::class.java)
        ?.flatten()
        ?.toList()
        ?: throw RuntimeException(
            "Vi fikk ingen resultater da vi forsøkte å hente testresultater fra ${uri}")
  }

  fun fetchResultatAggregering(uri: URI, resultatType: ResultatUrls): List<AutotesterTestresultat> {
    return restTemplate.getForObject(uri, getAggregationClass(resultatType))?.toList()
        ?: throw RuntimeException(
            "Vi fikk ingen resultater da vi forsøkte å hente testresultater fra ${uri}")
  }

  private fun getAggregationClass(
      resultatType: ResultatUrls
  ): Class<out Array<out AutotesterTestresultat>> {
    val returnCLass =
        when (resultatType) {
          ResultatUrls.urlAggreggeringTR -> Array<AggregertResultatTestregel>::class.java
          ResultatUrls.urlAggregeringSK -> Array<AggregertResultatSuksesskriterium>::class.java
          ResultatUrls.urlAggregeringSide -> Array<AggregertResultatSide>::class.java
          ResultatUrls.urlAggregeringSideTR -> Array<AggregertResultatSideTestregel>::class.java
          else -> Array<AggregertResultatTestregel>::class.java
        }
    return returnCLass
  }

  data class CustomStatus(val testaSider: Int, val talSider: Int)

  data class AutoTesterLenker(
      val urlFulltResultat: URL,
      val urlBrot: URL,
      val urlAggregeringTR: URL,
      val urlAggregeringSK: URL,
      val urlAggregeringSide: URL,
      val urlAggregeringSideTR: URL,
      val urlAggregeringLoeysing: URL
  )

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "runtimeStatus")
  @JsonSubTypes(
      JsonSubTypes.Type(value = AutoTesterStatus.Pending::class, name = "Pending"),
      JsonSubTypes.Type(value = AutoTesterStatus.Running::class, name = "Running"),
      JsonSubTypes.Type(value = AutoTesterStatus.Completed::class, name = "Completed"),
      JsonSubTypes.Type(value = AutoTesterStatus.Failed::class, name = "Failed"),
      JsonSubTypes.Type(value = AutoTesterStatus.Terminated::class, name = "Terminated"),
      JsonSubTypes.Type(value = AutoTesterStatus.Other::class, name = "ContinuedAsNew"),
      JsonSubTypes.Type(value = AutoTesterStatus.Other::class, name = "Suspended"))
  sealed class AutoTesterStatus {
    object Pending : AutoTesterStatus()

    data class Running(val customStatus: CustomStatus?) : AutoTesterStatus()

    data class Completed(val output: AutoTesterLenker) : AutoTesterStatus()

    data class Failed(val output: String) : AutoTesterStatus()

    object Terminated : AutoTesterStatus()

    data class Other(val output: String?) : AutoTesterStatus()
  }

  data class StatusUris(val statusQueryGetUri: URI)

  enum class ResultatUrls {
    urlFulltResultat,
    urlBrot,
    urlAggreggeringTR,
    urlAggregeringSK,
    urlAggregeringSide,
    urlAggregeringSideTR,
    urlAggregeringLoeysing
  }

  data class AutotestingStatus(
      val loeysing: Loeysing,
      val statusUrl: URL,
      val antallNettsider: Int
  )
}
