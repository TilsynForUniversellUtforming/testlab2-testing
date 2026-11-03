package no.uutilsynet.testlab2testing.testresultat.model

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatStatus

class TestObjekt(
    val testregelId: Int,
    val sideutvalId: Int,
    val testgrunnlagId: Int,
    val testresultat:List<ResultatManuellKontroll>,
    val status: ResultatStatus
)
{

    val resultatSideNivaa: TestresultatUtfall?
        get() {
            val testrestultatSideNivaa = testresultat.single { it.element?.elementOmtale == "Side" }
            requireNotNull(testrestultatSideNivaa.element)
            { "Testresultat for sideutvalId $sideutvalId har ikkje resultat på side-nivå" }
            return testrestultatSideNivaa.element.elementResultat
        }

    val utfallSideNivaa: String?
        get() {
            val testrestultatSideNivaa = testresultat.single { it.element?.elementOmtale == "Side" }
            requireNotNull(testrestultatSideNivaa.element)
            { "Testresultat for sideutvalId $sideutvalId har ikkje resultat på side-nivå" }
            return testrestultatSideNivaa.element.elementUtfall
        }
}


