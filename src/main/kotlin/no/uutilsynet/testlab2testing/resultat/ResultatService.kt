package no.uutilsynet.testlab2testing.resultat

import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.*
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class ResultatService(
    val maalingResource: MaalingResource,
    val testregelDAO: TestregelDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
    val kravregisterClient: KravregisterClient
) {

  fun getResultatForAutomatiskMaaling(
      maalingId: Int,
      loeysingId: Int?
  ): List<TestresultatDetaljert> {

    val testresultat: List<AutotesterTestresultat>? =
        maalingResource.getTestresultat(maalingId, loeysingId)?.getOrThrow()

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
      sakId: Int,
      testregelNoekkel: String?,
      loeysingId: Int?
  ): List<TestresultatDetaljert> {
    val testresultat = testResultatDAO.getManyResults(sakId).getOrThrow()

    val filterTestregelId = getTestregelIdFromSchema(testregelNoekkel.toString())

    return testresultat
        .filter { filterByTestregel(it.testregelId, filterTestregelId) }
        .filter { filterByLoeysing(it.loeysingId, loeysingId) }
        .map {
          val testregel: Testregel = getTestregel(it.testregelId)
          // it.testregel er databaseId ikkje feltet testregelId i db
          TestresultatDetaljert(
              it.id,
              it.loeysingId,
              it.testregelId,
              testregel.testregelId,
              it.testgrunnlagId,
              URI(getUrlFromNettside(it.nettsideId)).toURL(),
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

  fun getUrlFromNettside(nettsideId: Int): String {
    return sideutvalDAO.getNettside(nettsideId).let { it?.url ?: "" }
  }

  fun testVartUtfoertToLocalTime(testVartUtfoert: Instant?): LocalDateTime? {
    return testVartUtfoert?.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDateTime()
  }

  fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
    return kravregisterClient.getWcagKrav(kravId).getOrNull()?.suksesskriterium?.let { listOf(it) }
        ?: emptyList()
  }
}
