package no.uutilsynet.testlab2testing.inngaendekontroll

import java.net.URI
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagKontrollDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestgrunnlagDAOTest(
    @Autowired val testgrunnlagDAO: TestgrunnlagKontrollDAO,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired val testregelDAO: TestregelDAO,
) {

  private var kontrollId: Int by Delegates.notNull()
  private var utvalId: Int by Delegates.notNull()

  var testKontroll: KontrollDAO.KontrollDB? = null

  @BeforeAll
  fun setUp() {
    testKontroll = opprettTestsak()
  }

  @AfterAll
  fun cleaup() {
    utvalDAO.deleteUtval(utvalId)
  }

  @Test
  fun createTestgrunnlag() {
    val nyttTestgrunnlagManuell =
        NyttTestgrunnlag(
            parentId = testKontroll!!.id,
            namn = "Testgrunnlag",
            type = TestgrunnlagType.OPPRINNELEG_TEST,
            sideutval = testKontroll!!.sideutval,
            testregelIdList = testKontroll?.testreglar?.testregelIdList ?: emptyList())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)
    assertDoesNotThrow { id }
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).getOrThrow()

    assertThat(testgrunnlag).isNotNull
    assertThat(testgrunnlag.namn).isEqualTo("Testgrunnlag")
    assertThat(testgrunnlag.testreglar.map { it.id }).isEqualTo(listOf(1))
    assertThat(testgrunnlag.sideutval).isEqualTo(nyttTestgrunnlagManuell.sideutval)
    assertThat(testgrunnlag.type).isEqualTo(TestgrunnlagType.OPPRINNELEG_TEST)
  }

  @Test
  fun testUpdateTestgrunnlag() {
    val nyttTestgrunnlagManuell =
        NyttTestgrunnlag(
            parentId = testKontroll!!.id,
            namn = "Testgrunnlag",
            type = TestgrunnlagType.OPPRINNELEG_TEST,
            sideutval = testKontroll!!.sideutval,
            testregelIdList = testKontroll?.testreglar?.testregelIdList ?: emptyList())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)
    assertDoesNotThrow { id }
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).getOrThrow()

    val oppdatertTestgrunlag =
        testgrunnlag.copy(namn = "Oppdatert testgrunnlag", testreglar = listOf())

    val result = testgrunnlagDAO.updateTestgrunnlag(oppdatertTestgrunlag)

    assertThat(result.getOrThrow().testreglar.map { it.id })
        .isEqualTo(oppdatertTestgrunlag.testreglar.map { it.id })
  }

  @Test
  fun testSlettTestgrunnlag() {
    val nyttTestgrunnlagManuell =
        NyttTestgrunnlag(
            parentId = testKontroll!!.id,
            namn = "Testgrunnlag",
            type = TestgrunnlagType.OPPRINNELEG_TEST,
            sideutval = testKontroll!!.sideutval,
            testregelIdList = testKontroll?.testreglar?.testregelIdList ?: emptyList())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)

    testgrunnlagDAO.deleteTestgrunnlag(id.getOrThrow())

    assertThat(testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).isFailure).isTrue()

    assertThat(testgrunnlagDAO.getTestgrunnlagForKontroll(testKontroll!!.id, null).isEmpty())
  }

  private fun opprettTestsak(): KontrollDAO.KontrollDB {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll", "Ola Nordmann", Kontroll.Sakstype.Arkivsak, "1234")

    kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            Kontroll.Kontrolltype.InngaaendeKontroll,
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
    val testregel = testregelDAO.getTestregelList().first()
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregel.id))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))

    return kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
  }
}
