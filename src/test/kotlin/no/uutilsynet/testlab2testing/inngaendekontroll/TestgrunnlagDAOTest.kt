package no.uutilsynet.testlab2testing.inngaendekontroll

import java.net.URI
import kotlin.properties.Delegates
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestgrunnlagDAOTest(
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired private val testUtils: TestUtils,
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
            kontrollId = testKontroll!!.id,
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
            kontrollId = testKontroll!!.id,
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
            kontrollId = testKontroll!!.id,
            namn = "Testgrunnlag",
            type = TestgrunnlagType.OPPRINNELEG_TEST,
            sideutval = testKontroll!!.sideutval,
            testregelIdList = testKontroll?.testreglar?.testregelIdList ?: emptyList())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)

    testgrunnlagDAO.deleteTestgrunnlag(id.getOrThrow())

    assertThat(testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).isFailure).isTrue()

    assertThat(
        listOf(testgrunnlagDAO.getTestgrunnlagForKontroll(testKontroll!!.id).opprinneligTest)
            .isEmpty())
  }

  private fun opprettTestsak(): KontrollDAO.KontrollDB {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Sakstype.Arkivsak,
            "1234",
            Kontrolltype.InngaaendeKontroll)

    kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            Kontrolltype.InngaaendeKontroll,
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
    val testregel = testUtils.createTestregel()
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregel))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))

    return kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
  }
}
