package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.testregel.TestregelDAO

sealed class KontrollResultatService(val testregelDAO: TestregelDAO) {

    protected fun getTestreglarForKrav(kravId: Int): List<Int> {
        val testregelIds: List<Int> = testregelDAO.getTestregelForKrav(kravId).map { it.id }
        return testregelIds
    }
    protected fun filterTestregelNoekkel(testregelNoekkel: String, testregelId: List<Int>): Boolean {
        return testregelId.contains(getTestregelIdFromSchema(testregelNoekkel) ?: 0)
    }

    protected fun getTestregelIdFromSchema(testregelKey: String): Int? {
        testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
            return testregel?.id
        }
    }

}