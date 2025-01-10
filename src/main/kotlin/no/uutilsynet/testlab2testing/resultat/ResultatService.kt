package no.uutilsynet.testlab2testing.resultat

import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class ResultatService(
    val resultatDAO: ResultatDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val kontrollDAO: KontrollDAO,
    val eksternResultatDAO: EksternResultatDAO,
    val automatiskResultatService: AutomatiskResultatService,
    val manueltResultatService: ManueltResultatService,
    val testregelService: TestregelService
) {

  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
    return automatiskResultatService.getResultatForMaaling(maalingId, loeysingId)
  }

  fun getResulatForManuellKontroll(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): List<TestresultatDetaljert> {
    return manueltResultatService.getResulatForManuellKontroll(kontrollId, loeysingId, kravId)
  }

  fun getResultatList(type: Kontrolltype?): List<Resultat> {
    return when (type) {
      Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat()
      Kontrolltype.InngaaendeKontroll,
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.Statusmaaling -> getManuellKontrollResultat()
      else -> getKontrollResultat()
    }
  }

  private fun getAutomatiskKontrollResultat(): List<Resultat> {
    return automatiskResultatService
        .getKontrollResultatAlleMaalingar()
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun getAutomatiskKontrollResultat(kontrollId: Int): List<Resultat> {
    return automatiskResultatService
        .getKontrollResultat(kontrollId)
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun getManuellKontrollResultat(kontrollId: Int): List<Resultat> {
    return manueltResultatService
        .getKontrollResultat(kontrollId)
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun getManuellKontrollResultat(): List<Resultat> {
    return manueltResultatService
        .getKontrollResultatAlleTestgrunnlag()
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun getKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getAllResultat()
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
        .map { it.copy(loeysingar = limitResultatList(it.loeysingar)) }
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultat(kontrollId: Int): List<Resultat> {
    return resultatDAO
        .getResultatKontroll(kontrollId)
        .groupBy { it.testgrunnlagId }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultatMedType(kontrollId: Int, kontrolltype: Kontrolltype): List<Resultat> {
    val resultat = resultatForKontrollType(kontrolltype, kontrollId)
    return resultat
  }

  private fun resultatForKontrollType(kontrolltype: Kontrolltype, kontrollId: Int): List<Resultat> {
    val resultat =
        when (kontrolltype) {
          Kontrolltype.Statusmaaling,
          Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat(kontrollId)
          Kontrolltype.InngaaendeKontroll,
          Kontrolltype.Tilsyn,
          Kontrolltype.Uttalesak, -> getManuellKontrollResultat(kontrollId)
        }
    return resultat
  }

  private fun resultatgruppertPrKontroll(
      kontrollId: Int,
      result: List<ResultatLoeysingDTO>
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
    val resultatLoeysingar =
        result
            .groupBy { it.testgrunnlagId }
            .map { (id, result) ->
              resultatForLoeysingarPrTestgrunnlag(result, id, getKontrolltype(result))
            }
            .flatten()
    return resultatLoeysingar
  }

  private fun resultatForLoeysingarPrTestgrunnlag(
      result: List<ResultatLoeysingDTO>,
      testgrunnlagId: Int,
      kontrolltype: Kontrolltype
  ): List<LoeysingResultat> {
    val loeysingar = getLoeysingMap(mapResultatToLoeysingId(result)).getOrThrow()
    val statusLoeysingar = progresjonPrLoeysing(testgrunnlagId, kontrolltype, loeysingar)
    val resultatLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)
    return resultatLoeysingar
  }

  private fun mapResultatToLoeysingId(result: List<ResultatLoeysingDTO>) =
      result.map { it.loeysingId }

  private fun erKontrollPublisert(result: List<ResultatLoeysingDTO>) =
      eksternResultatDAO.erKontrollPublisert(result.first().testgrunnlagId, getKontrolltype(result))

  private fun resultatPrLoeysing(
      result: List<ResultatLoeysingDTO>,
      loeysingar: LoysingList,
      statusLoeysingar: Map<Int, Int>,
  ): List<LoeysingResultat> {
    val resultLoeysingar =
        result
            .groupBy { it.loeysingId }
            .map { (loeysingId, resultLoeysing) ->
              LoeysingResultat(
                  loeysingId,
                  loeysingar.getNamn(loeysingId),
                  loeysingar.getVerksemdNamn(loeysingId),
                  calculateScore(resultLoeysing),
                  resultLoeysing.first().testType,
                  talTestaElementDTO(resultLoeysing),
                  calculateTalElementSamsvar(resultLoeysing),
                  calculateTalElementBrot(resultLoeysing),
                  resultLoeysing.first().testar,
                  statusLoeysingar[loeysingId] ?: 0)
            }

    return resultLoeysingar
  }

  private fun limitResultatList(resultLoeysingar: List<LoeysingResultat>): List<LoeysingResultat> {
    return if (resultLoeysingar.size > 5) {
      resultLoeysingar.subList(0, 5)
    } else resultLoeysingar
  }

  private fun calculateTalElementBrot(resultLoeysing: List<ResultatLoeysingDTO>) =
      resultLoeysing.sumOf { it.talElementBrot }

  private fun talTestaElementDTO(resultLoeysing: List<ResultatLoeysingDTO>) =
      calculateTalElementSamsvar(resultLoeysing) + calculateTalElementSamsvar(resultLoeysing)

  private fun calculateTalElementSamsvar(resultLoeysing: List<ResultatLoeysingDTO>) =
      resultLoeysing.sumOf { it.talElementSamsvar }

  private fun calculateScore(resultLoeysing: List<ResultatLoeysingDTO>) =
      resultLoeysing.filter { filterIkkjeForekomst(it) }.map { it.score }.average()

  private fun filterIkkjeForekomst(it: ResultatLoeysingDTO) =
      !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar)

  private fun getKontrolltype(result: List<ResultatLoeysingDTO>) = result.first().typeKontroll

  private fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      kontrolltype: Kontrolltype,
      loeysingar: LoysingList
  ): Map<Int, Int> {
    if (kontrolltype == Kontrolltype.ForenklaKontroll) {
      return loeysingar.loeysingar.keys.associateWith { 100 }
    }

    val resultatPrTestgrunnlag = manueltResultatService.getResultatPrTestgrunnlag(testgrunnlagId)
    val progresjon =
        resultatPrTestgrunnlag
            .groupBy { it.loeysingId }
            .entries
            .map { (loeysingId, result) -> Pair(loeysingId, percentageFerdig(result)) }
            .associateBy({ it.first }, { it.second })

    return progresjon
  }

  private fun percentageFerdig(result: List<ResultatManuellKontroll>): Int =
      (result
              .map { it.status }
              .count { it == ResultatManuellKontrollBase.Status.Ferdig }
              .toDouble() / result.size)
          .times(100)
          .toInt()

  private fun getLoeysingMap(listLoysingId: List<Int>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(listLoysingId).mapCatching { loeysingList ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  fun getKontrollLoeysingResultat(
      kontrollId: Int,
      loeysingId: Int
  ): List<ResultatOversiktLoeysing> {

    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .toResultatOversiktLoeysing()
  }

  fun getKontrollLoeysingResultatIkkjeRetest(
      kontrollId: Int,
      loeysingId: Int
  ): List<ResultatOversiktLoeysing> {
    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .filter { it.testType == TestgrunnlagType.OPPRINNELEG_TEST }
        .toResultatOversiktLoeysing()
  }

  private fun erIkkjeForekomst(talElementBrot: Int, talElementSamsvar: Int): Boolean {
    return talElementBrot == 0 && talElementSamsvar == 0
  }

  private fun handleIkkjeForekomst(resultat: ResultatOversiktLoeysing): ResultatOversiktLoeysing {
    if (erIkkjeForekomst(resultat.talElementBrot, resultat.talElementSamsvar)) {
      return resultat.copy(score = null)
    }
    return resultat
  }

  private fun mapKrav(result: ResultatLoeysingDTO): ResultatLoeysing {
    val krav = getKravWcag2x(result)

    return ResultatLoeysing(
        id = result.id,
        testgrunnlagId = result.testgrunnlagId,
        namn = result.namn,
        typeKontroll = result.typeKontroll,
        testType = result.testType,
        dato = result.dato,
        testar = result.testar,
        loeysingId = result.loeysingId,
        score = result.score,
        talElementSamsvar = result.talElementSamsvar,
        talElementBrot = result.talElementBrot,
        testregelId = result.testregelId,
        kravId = krav.id,
        kravTittel = krav.tittel)
  }

  private fun getKravWcag2x(result: ResultatLoeysingDTO): KravWcag2x {
    return testregelService.getKravWcag2x(result.id)
  }

  fun getResultatListKontroll(
      kontrollId: Int,
      loeysingId: Int,
      kravid: Int
  ): List<TestresultatDetaljert> {
    val typeKontroll =
        kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first().kontrolltype
    return when (typeKontroll) {
      Kontrolltype.ForenklaKontroll ->
          automatiskResultatService.getResultatForAutomatiskKontroll(kontrollId, loeysingId, kravid)
      Kontrolltype.InngaaendeKontroll,
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.Statusmaaling -> getResulatForManuellKontroll(kontrollId, loeysingId, kravid)
    }
  }

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrolltype: Kontrolltype?,
      loeysingId: Int?,
      startDato: LocalDate?,
      sluttDato: LocalDate?
  ): List<ResultatTema> =
      resultatDAO.getResultatPrTema(kontrollId, kontrolltype, loeysingId, startDato, sluttDato)

  fun getResultatPrKrav(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      loeysingId: Int?,
      fraDato: LocalDate?,
      tilDato: LocalDate?
  ): List<ResultatKrav> {
    return resultatDAO
        .getResultatPrKrav(kontrollId, kontrollType, loeysingId, fraDato, tilDato)
        .map { it.toResultatKrav() }
  }

  class LoysingList(val loeysingar: Map<Int, Loeysing.Expanded>) {
    fun getNamn(loeysingId: Int): String {
      val loeysing = loeysingar[loeysingId]
      return loeysing?.namn ?: ""
    }

    fun getVerksemdNamn(loeysingId: Int): String {
      val loeysing = loeysingar[loeysingId]
      if (loeysing?.verksemd == null) return ""
      return loeysing.verksemd.namn
    }
  }

  fun ResultatKravBase.toResultatKrav(): ResultatKrav {
    return ResultatKrav(
        suksesskriterium = testregelService.getSuksesskriteriumFromKrav(kravId),
        score = score,
        talTestaElement =
            talElementBrot + talElementSamsvar + talElementVarsel + talElementIkkjeForekomst,
        talElementBrot = talElementBrot,
        talElementSamsvar = talElementSamsvar,
        talElementVarsel = talElementVarsel,
        talElementIkkjeForekomst = talElementIkkjeForekomst)
  }

  private fun List<ResultatLoeysingDTO>.toResultatOversiktLoeysing():
      List<ResultatOversiktLoeysing> {

    val loeysingar = this.map { it.loeysingId }.let { getLoeysingMap(it).getOrThrow() }

    return this.map { krav -> mapKrav(krav) }
        .groupBy { it.kravId }
        .map { (kravId, result) ->
          ResultatOversiktLoeysing(
                  result.first().loeysingId,
                  loeysingar.getNamn(result.first().loeysingId),
                  result.first().typeKontroll,
                  result.first().namn,
                  result.map { it.testar }.flatten().distinct(),
                  result.map { it.score }.average(),
                  kravId,
              result.first().kravTittel,
                  talTestaElement(result),
                  result.sumOf { it.talElementBrot },
                  result.sumOf { it.talElementSamsvar })
              .let { handleIkkjeForekomst(it) }
        }
  }

  private fun talTestaElement(result: List<ResultatLoeysing>) =
      result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar }
}
