package no.uutilsynet.testlab2testing.resultat.external

import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.resultat.ResultatMetadata
import no.uutilsynet.testlab2testing.resultat.export.ResultatRegisterProperties
import no.uutilsynet.testlab2testing.resultat.external.model.Testgrunnlag
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.body


@Service
class ResultatMetadataClient(restTemplate: RestTemplate,
                             private val resultatRegisterProperties: ResultatRegisterProperties,
    private val kontrollDAO: KontrollDAO) {

    val restClient = RestClient.create(restTemplate)

    fun getTestgrunnlagForKontroll(kontroll: Int): List<Testgrunnlag> {
        val url = "${resultatRegisterProperties.host}/testgrunnlag/search/findByKontroll?kontroll=$kontroll"
        val testgrunnlag = restClient.get().uri(url)
            .retrieve()
            .body<Array<Testgrunnlag>>()

        return testgrunnlag?.toList() ?: emptyList();
    }

    fun     getResultatMetadata(kontrollId: Int): List<ResultatMetadata> {
        val url = "${resultatRegisterProperties.host}/resultatmetadata/kontrollid/$kontrollId"
        val resultatMetadata = restClient.get().uri(url)
            .retrieve()
            .body<Array<ResultatMetadata>>()

        val kontrollMetadata = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
        return resultatMetadata?.map {
            it.copy(
                kontrollNamn = kontrollMetadata.tittel,
                kontrollType = kontrollMetadata.kontrolltype)

        }?.toList() ?: emptyList();
    }
}