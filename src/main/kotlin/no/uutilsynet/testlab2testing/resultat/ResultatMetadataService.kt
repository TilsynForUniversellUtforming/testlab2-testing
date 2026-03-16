package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestgrunnlagType
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ResultatMetadataService(private val kontrollDAO: KontrollDAO,private val testgrunnlagService: TestgrunnlagService, private val maalingService: MaalingService) {


}

data class ResultatMetadata(
    var kontrollId: Int,
    val kontrollNamn: String,
    val kontrollType: Kontrolltype,
    var testgrunnlagId: Int,
    val testrunUuid: String,
    val testgrunnlagType: TestgrunnlagType,
    val dato: LocalDate
)