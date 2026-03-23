package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.resultat.external.ResultatMetadataClient
import no.uutilsynet.testlab2testing.resultat.repository.ResultatDAO
import org.springframework.stereotype.Service
import java.time.LocalDate


@Service
class ResultatMetadataService(
    private val resultatMetadataClient: ResultatMetadataClient,
    private val kontrollDAO: KontrollDAO,
    private val resultatDAO: ResultatDAO
) {

    fun hentResultatMetadata(kontrollId: Int, loeysingId: Int?): List<ResultatMetadata> {
        return resultatDAO.getResultatMetadata(kontrollId, loeysingId)
            .ifEmpty {
            resultatMetadataClient.getResultatMetadata(kontrollId)
        }
}
}


data class ResultatMetadata(
    var kontrollId: Int,
    val kontrollNamn: String,
    val kontrollType: Kontrolltype,
    var testgrunnlagId: Int?,
    val testrunUuid: String,
    val testgrunnlagType: TestgrunnlagType,
    val dato: LocalDate,
    val testar: List<String>?
)