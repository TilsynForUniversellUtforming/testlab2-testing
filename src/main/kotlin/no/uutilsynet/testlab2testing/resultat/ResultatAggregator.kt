package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype

object ResultatAggregator {
    fun getKontrollResultatCommon(
        fetchResults: () -> List<ResultatPerTestregelDTO>,
        resultatgruppertPrKontroll: (Int, List<ResultatPerTestregelDTO>) -> Resultat
    ): List<Resultat> {
        return fetchResults()
            .groupBy { it.id }
            .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
    }

    fun loeysingResultatList(
        result: List<ResultatPerTestregelDTO>,
        resultatForLoeysingarPrTestgrunnlag:
            (List<ResultatPerTestregelDTO>, Int, Kontrolltype) -> List<LoeysingResultat>
    ): List<LoeysingResultat> {
        return result
            .filter { it.testgrunnlagId != null }
            .groupBy { it.testgrunnlagId }
            .map { (id, result) ->
                resultatForLoeysingarPrTestgrunnlag(result, id!!, getKontrolltype(result))
            }
            .flatten()
    }

    private fun getKontrolltype(result: List<ResultatPerTestregelDTO>) = result.first().typeKontroll


    // Add other aggregation/grouping functions as needed
}

