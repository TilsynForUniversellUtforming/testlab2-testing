package no.uutilsynet.testlab2testing.wordrapport

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.*
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

@Service
class WordRapportService(
    @Autowired val restTemplate: RestTemplate,
    @Autowired val wordRapportBuilder: WordRapportBuilder,
    @Autowired val testResultatDAO: TestResultatDAO,
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val properties: RapportVerktoeyKlient
) {

  private val logger = LoggerFactory.getLogger(WordRapportService::class.java)

  fun opprettRapport(kontrollId: Int, loeysingId: Int): WordRapport {

    val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
    val loeysing = loeysingsRegisterClient.getManyExpanded(listOf(loeysingId)).getOrThrow().first()

    val testgrunnlag = testgrunnlagDAO.getOpprinneligTestgrunnlag(kontroll.id).getOrThrow()
    val testresultat = testResultatDAO.getManyResults(testgrunnlag).getOrThrow()

    return wordRapportBuilder
        .kontroll(kontroll)
        .loeysing(loeysing)
        .testresultat(testresultat)
        .build()
  }

  fun createWordRapport(wordRapport: WordRapport): ByteArray {
    restTemplate.messageConverters.add(0, ByteArrayHttpMessageConverter())

    val restClient = RestClient.builder(restTemplate).build()

    val requestBody =
        mapOf(
            "rapportNummer" to wordRapport.rapportNummer,
            "datoFra" to wordRapport.datoFra,
            "datoTil" to wordRapport.datoTil,
            "verksemd" to wordRapport.verksemd,
            "loeysing" to wordRapport.loeysing,
            "avvik" to wordRapport.avvik)

    val response =
        restClient
            .post()
            .uri(properties.host + "/rapport/lagRapport")
            .body(requestBody)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(
                MediaType.valueOf(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, response ->
              logger.error(response.body.readAllBytes().contentToString())
            }
            .body(ByteArray::class.java)

    return response ?: throw IllegalStateException("No response body")
  }
}

@ConfigurationProperties(prefix = "rapportverktoey")
data class RapportVerktoeyKlient(val host: String)

data class Testresponse(val message: String, val number: Int)
