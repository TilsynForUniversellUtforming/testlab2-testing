package no.uutilsynet.testlab2testing.resultat.resultMappers

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.KontrollResultatService
import no.uutilsynet.testlab2testing.resultat.KontrollResultatServiceFactory
import no.uutilsynet.testlab2testing.resultat.LoeysingResultat
import no.uutilsynet.testlab2testing.resultat.LoysingList
import no.uutilsynet.testlab2testing.resultat.Resultat
import no.uutilsynet.testlab2testing.resultat.ResultatLoeysingDTO
import org.springframework.stereotype.Component
import kotlin.collections.component1
import kotlin.collections.component2

@Component
class ResultKontrollMapper(
    private val resultatCalculator: ResultatCalculator,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val eksternResultatDAO: EksternResultatDAO,
    private val kontrollResultatServiceFactory: KontrollResultatServiceFactory,
    ) {

    fun getKontrollResultatCommon(
        fetchResults: () -> List<ResultatLoeysingDTO>,
    ): List<Resultat> {
        return fetchResults()
            .groupBy { it.id }
            .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
    }

    private fun resultatgruppertPrKontroll(
        kontrollId: Int,
        result: List<ResultatLoeysingDTO>,
    ): Resultat {
        val resultatLoeysingar = loeysingResultatList(result)
        val publisert = erKontrollPublisert(result)

        return Resultat(
            kontrollId,
            result.first().namn,
            getKontrolltype(result),
            resultatLoeysingar.first().testType,
            result.first().dato,
            publisert,
            resultatLoeysingar)
    }

    private fun loeysingResultatList(result: List<ResultatLoeysingDTO>): List<LoeysingResultat> {
        return result
            .groupBy { it.testgrunnlagId }
            .map { (id, result) ->
                resultatForLoeysingarPrTestgrunnlag(result, id, getKontrolltype(result))
            }
            .flatten()
    }

    private fun resultatForLoeysingarPrTestgrunnlag(
        result: List<ResultatLoeysingDTO>,
        testgrunnlagId: Int,
        kontrolltype: Kontrolltype,
    ): List<LoeysingResultat> {
        val loeysingar = getLoeysingMap(mapResultatToLoeysingId(result)).getOrThrow()
        val statusLoeysingar = progresjonPrLoeysing(testgrunnlagId, kontrolltype, loeysingar)
        return resultatPrLoeysing(result, loeysingar, statusLoeysingar)
    }

    private fun resultatPrLoeysing(
        result: List<ResultatLoeysingDTO>,
        loeysingar: LoysingList,
        statusLoeysingar: Map<Int, Int>,
    ): List<LoeysingResultat> {
        val testarar = getBrukararForTest(result.first().id)

        return result
            .groupBy { it.loeysingId }
            .map { (loeysingId, resultLoeysing) ->
                LoeysingResultat(
                    loeysingId,
                    loeysingar.getNamn(loeysingId),
                    loeysingar.getVerksemdNamn(loeysingId),
                    loeysingar.getOrgnr(loeysingId),
                    resultatCalculator.scoreOrZero(resultLoeysing),
                    resultLoeysing.first().testType,
                    resultatCalculator.talTestaElementFraDto(resultLoeysing),
                    resultatCalculator.talElementSamsvar(resultLoeysing),
                    resultatCalculator.talElementBrot(resultLoeysing),
                    testarar,
                    statusLoeysingar[loeysingId] ?: 0)
            }
    }

    private fun progresjonPrLoeysing(
        testgrunnlagId: Int,
        kontrolltype: Kontrolltype,
        loeysingar: LoysingList,
    ): Map<Int, Int> {
        return getResultatServiceByKontrollType(kontrolltype).progresjonPrLoeysing(testgrunnlagId, loeysingar)
    }

    private fun getLoeysingMap(listLoysingId: List<Int>): Result<LoysingList> {
        return loeysingsRegisterClient.getManyExpanded(listLoysingId).mapCatching { loeysingList ->
            LoysingList(loeysingList.associateBy { it.id })
        }
    }

    private fun getResultServiceById(kontrollId: Int): KontrollResultatService {
        return kontrollResultatServiceFactory.getResultatService(kontrollId)
    }

    private fun getResultatServiceByKontrollType(kontrollType: Kontrolltype): KontrollResultatService {
        return kontrollResultatServiceFactory.getResultatService(kontrollType)
    }

    private fun getBrukararForTest(kontrollId: Int): List<String> {
        return getResultServiceById(kontrollId).getBrukararForTest(kontrollId)
    }

    private fun erKontrollPublisert(result: List<ResultatLoeysingDTO>) =
        eksternResultatDAO.erKontrollPublisert(result.first().id, getKontrolltype(result))

    private fun getKontrolltype(result: List<ResultatLoeysingDTO>) = result.first().typeKontroll

    private fun mapResultatToLoeysingId(result: List<ResultatLoeysingDTO>) =
        result.map { it.loeysingId }


}