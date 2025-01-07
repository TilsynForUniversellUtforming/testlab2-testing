package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.forenkletkontroll.TestResultat
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Service

@Service
class AutomatiskResultatService(val maalingDAO: MaalingDAO, val maalingService: MaalingService,
                                testregelDAO: TestregelDAO
) : KontrollResultatService(testregelDAO) {

    fun getResultatForAutomatiskKontroll(
        kontrollId: Int,
        loeysingId: Int,
        kravId: Int
    ): List<TestresultatDetaljert> {
        val maalingId = getMaalingForKontroll(kontrollId)
        val testregelIds: List<Int> = getTestreglarForKrav(kravId)

        val testresultat: List<TestResultat> = getAutotesterTestresultat(maalingId, loeysingId)

        if (testresultat.isNotEmpty()) {
            return testresultat
                .filter { filterTestregelNoekkel(it.testregelId, testregelIds) }
                .map { testresultatDetaljertMaaling(it, maalingId) }
        }
        return emptyList()
    }

    private fun getAutotesterTestresultat(maalingId: Int, loeysingId: Int?): List<TestResultat> {
        val testresultat: List<TestResultat> =
            maalingService.getTestresultatMaalingLoeysing(maalingId, loeysingId).getOrThrow().map {
                it as TestResultat
            }
        return testresultat
    }

    private fun getMaalingForKontroll(kontrollId: Int): Int {
        val maalingId =
            maalingDAO.getMaalingIdFromKontrollId(kontrollId)
                ?: throw RuntimeException("Fant ikkje maalingId for kontrollId $kontrollId")
        return maalingId
    }

    fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
        return getAutotesterTestresultat(maalingId, loeysingId).map {
            testresultatDetaljertMaaling(it, maalingId)
        }
    }

    private fun testresultatDetaljertMaaling(it: TestResultat, maalingId: Int) =
        TestresultatDetaljert(
            null,
            it.loeysingId,
            getTestregelIdFromSchema(it.testregelId) ?: 0,
            it.testregelId,
            maalingId,
            it.side,
            it.suksesskriterium,
            it.testVartUtfoert,
            it.elementUtfall,
            it.elementResultat,
            it.elementOmtale,
            null,
            null,
            emptyList())

}