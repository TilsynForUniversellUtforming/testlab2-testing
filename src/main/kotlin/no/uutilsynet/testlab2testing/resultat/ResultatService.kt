package no.uutilsynet.testlab2testing.resultat

import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.AutotesterTestresultat
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingResource
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.TestResultat
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class ResultatService(
    val maalingResource: MaalingResource,
    val testregelDAO: TestregelDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
    val kravregisterClient: KravregisterClient,
    val resultatDAO: ResultatDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient
) {

  fun getResultatForAutomatiskMaaling(
      maalingId: Int,
      loeysingId: Int?
  ): List<TestresultatDetaljert> {

    val testresultat: List<AutotesterTestresultat>? =
        maalingResource.getTestresultat(maalingId, loeysingId).getOrThrow()

    if (!testresultat.isNullOrEmpty() && testresultat.first() is TestResultat) {
      return testresultat
          .map { it as TestResultat }
          .map {
            TestresultatDetaljert(
                null,
                it.loeysingId,
                getTestregelIdFromSchema(it.testregelId).let { id -> id ?: 0 },
                it.testregelId,
                maalingId,
                it.side,
                it.suksesskriterium,
                it.testVartUtfoert,
                it.elementUtfall,
                it.elementResultat,
                it.elementOmtale,
                null)
          }
    }
    return emptyList()
  }

  fun getResulatForManuellKontroll(
      testgrunnlagId: Int,
      testregelNoekkel: String?,
      loeysingId: Int?
  ): List<TestresultatDetaljert> {
    val testresultat = testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()

    val sideutvalIds = testresultat.map { it.sideutvalId }.distinct()
    val sideutvalIdUrlMap: Map<Int, URL> = sideutvalDAO.getSideutvalUrlMapKontroll(sideutvalIds)

    val filterTestregelId = getTestregelIdFromSchema(testregelNoekkel.toString())

    return testresultat
        .filter { filterByTestregel(it.testregelId, filterTestregelId) }
        .filter { filterByLoeysing(it.loeysingId, loeysingId) }
        .map {
          val testregel: Testregel = getTestregel(it.testregelId)
          val url = sideutvalIdUrlMap[it.sideutvalId]
          if (url == null) {
            throw IllegalArgumentException("Ugyldig testresultat")
          }

          // it.testregel er databaseId ikkje feltet testregelId i db
          TestresultatDetaljert(
              it.id,
              it.loeysingId,
              it.testregelId,
              testregel.testregelId,
              it.testgrunnlagId,
              url,
              getSuksesskriteriumFromTestregel(testregel.kravId),
              testVartUtfoertToLocalTime(it.testVartUtfoert),
              it.elementUtfall,
              it.elementResultat,
              TestresultatDetaljert.ElementOmtale(
                  htmlCode = null, pointer = null, description = it.elementOmtale),
              it.brukar)
        }
  }

  private fun filterByLoeysing(loeysingId: Int, loeysingIdFilter: Int?): Boolean {
    if (loeysingIdFilter != null) {
      return loeysingId == loeysingIdFilter
    }
    return true
  }

  fun filterByTestregel(testregelId: Int, testregelIdFilter: Int?): Boolean {
    if (testregelIdFilter != null) {
      return testregelId == testregelIdFilter
    }
    return true
  }

  fun getTestregel(idTestregel: Int): Testregel {
    testregelDAO.getTestregel(idTestregel).let { testregel ->
      return testregel ?: throw RuntimeException("Fant ikkje testregel med id $idTestregel")
    }
  }

  fun getTestregelIdFromSchema(testregelKey: String): Int? {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel?.id
    }
  }

  fun testVartUtfoertToLocalTime(testVartUtfoert: Instant?): LocalDateTime? {
    return testVartUtfoert?.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDateTime()
  }

  fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
    return kravregisterClient.getWcagKrav(kravId).getOrNull()?.suksesskriterium?.let { listOf(it) }
        ?: emptyList()
  }

  fun getResultatList(type: Kontroll.Kontrolltype?): List<Resultat> {
    return when (type) {
      Kontroll.Kontrolltype.InngaaendeKontroll -> getManuellKontrollResultat()
      Kontroll.Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat()
      else -> getKontrollResultat()
    }
  }

  private fun getAutomatiskKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getTestresultatMaaling()
        .groupBy { it.id }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun getKontrollResultat(): List<Resultat> {
    return resultatDAO.getResultat().groupBy { it.id }.map { (id, result) -> resultat(id, result) }
  }

  private fun getManuellKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getTestresultatTestgrunnlag()
        .groupBy { it.id }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun resultat(id: Int, result: List<ResultatLoeysing>): Resultat {
    val loeysingar = getLoeysingMap(result).getOrThrow()
    val statusLoeysingar = progresjonPrLoeysing(id, result.first().typeKontroll, loeysingar)
    val resultLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)

    return Resultat(
        id, result.first().namn, result.first().typeKontroll, result.first().dato, resultLoeysingar)
  }

  private fun resultatPrLoeysing(
      result: List<ResultatLoeysing>,
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
                  resultLoeysing.map { it.score }.average(),
                  resultLoeysing.first().testType,
                  resultLoeysing.map { it.talElementSamsvar }.sum(),
                  resultLoeysing.map { it.talElementBrot }.sum(),
                  getTestar(resultLoeysing.first().id, resultLoeysing.first().typeKontroll),
                  statusLoeysingar[loeysingId] ?: 0)
            }
    return resultLoeysingar
  }

  fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      Kontrolltype: Kontroll.Kontrolltype,
      loeysingar: LoysingList
  ): Map<Int, Int> {
    if (Kontrolltype == Kontroll.Kontrolltype.ForenklaKontroll) {
      return loeysingar.loeysingar.keys.associateWith { 100 }
    }
    val resultatPrSak = testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()
    return resultatPrSak
        .groupBy { it.loeysingId }
        .entries
        .map { (loeysingId, result) -> Pair(loeysingId, percentageFerdig(result)) }
        .associateBy({ it.first }, { it.second })
  }

  private fun percentageFerdig(result: List<ResultatManuellKontroll>): Int =
      (result.map { it.status }.count { it == ResultatManuellKontroll.Status.Ferdig } / result.size)
          .times(100)

  private fun getLoeysingMap(result: List<ResultatLoeysing>): Result<LoysingList> {
    return loeysingsRegisterClient.getMany(result.map { it.loeysingId }).mapCatching { loeysingList
      ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  private fun getTestar(testgrunnlagId: Int, kontrolltype: Kontroll.Kontrolltype): String {
    if (kontrolltype == Kontroll.Kontrolltype.InngaaendeKontroll) {
      return testResultatDAO.getBrukarForTestgrunnlag(testgrunnlagId)
    }
    return "testar"
  }

  class LoysingList(val loeysingar: Map<Int, Loeysing>) {
    fun getNamn(loeysingId: Int): String {
      val loeysing = loeysingar[loeysingId]
      return if (loeysing != null) {
        loeysing.namn
      } else {
        ""
      }
    }
  }
}
