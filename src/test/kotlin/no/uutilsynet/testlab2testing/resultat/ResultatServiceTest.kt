package no.uutilsynet.testlab2testing.resultat

import java.net.URI
import java.time.Instant
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResultatServiceTest(
    @Autowired val resultatService: ResultatService,
    @Autowired val aggregeringDAO: AggregeringDAO,
    @Autowired val testregelDAO: TestregelDAO,
    @Autowired val maalingDao: MaalingDAO,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val utvalDAO: UtvalDAO
) {

  private var kontrollId: Int by Delegates.notNull()
  private var utvalId: Int by Delegates.notNull()
  private var maalingId: Int by Delegates.notNull()
  private var testregelId: Int by Delegates.notNull()
  private var aggregertResultat: AggregeringPerTestregelDTO by Delegates.notNull()

  @AfterAll
  fun cleanup() {
    utvalDAO.deleteUtval(utvalId)
    kontrollDAO.deleteKontroll(kontrollId)
    maalingDao.deleteMaaling(maalingId)
  }

  @Test
  fun getTestresultatMaaling() {
    kontrollId = createTestKontroll()
    createTestMaaling()
    val resultat = resultatService.getResultatList(Kontroll.Kontrolltype.ForenklaKontroll)

    assert(resultat.isNotEmpty())

    val resultatKontroll = resultat[0]

    assertEquals(resultatKontroll.id, kontrollId)
    assertEquals(resultatKontroll.loeysingar.size, 1)
    assertEquals(resultatKontroll.loeysingar[0].score, 0.5)
  }

  fun createTestMaaling(): Int {
    val crawlParameters = CrawlParameters(10, 10)

    maalingId =
        maalingDao.createMaaling(
            "Testmaaling_resultat", Instant.now(), listOf(1), listOf(testregelId), crawlParameters)

    aggregertResultat =
        AggregeringPerTestregelDTO(
            maalingId, 1, testregelId, 1, arrayListOf(1, 2), 1, 2, 1, 1, 1, 1, 1, 0.5, 0.5, null)

    aggregeringDAO.createAggregertResultatTestregel(aggregertResultat)
    maalingDao.updateKontrollId(kontrollId, maalingId)

    return maalingId
  }

  private fun createTestKontroll(): Int {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Kontroll.Sakstype.Arkivsak,
            "1234",
            Kontroll.Kontrolltype.ForenklaKontroll)

    kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            Kontroll.Kontrolltype.ForenklaKontroll,
            opprettKontroll.tittel,
            opprettKontroll.saksbehandler,
            opprettKontroll.sakstype,
            opprettKontroll.arkivreferanse,
        )

    /* Add utval */
    val loeysingId = 1
    utvalId = utvalDAO.createUtval("test-skal-slettes", listOf(loeysingId)).getOrThrow()
    kontrollDAO.updateKontroll(kontroll, utvalId)

    /* Add testreglar */
    testregelId = testregelDAO.getTestregelList().first().id
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregelId))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))

    return kontrollId
  }
}
