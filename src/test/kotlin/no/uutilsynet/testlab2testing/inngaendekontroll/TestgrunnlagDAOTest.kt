package no.uutilsynet.testlab2testing.inngaendekontroll

import java.time.Instant
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.SakDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.Testgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.testregel.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestgrunnlagDAOTest(
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val sakDAO: SakDAO
) {

  var testSak: Sak? = null

  @BeforeAll
  fun setUp() {
    testSak = opprettTestsak()
  }

  @Test
  fun createTestgrunnlag() {
    val loeysingar = testSak?.loeysingar ?: emptyList()

    val nyttTestgrunnlagManuell =
        NyttTestgrunnlag(
            parentId = testSak?.id,
            namn = "Testgrunnlag",
            type = Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
            testregelar = listOf(1),
            loeysingar = loeysingar.first())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)
    assertDoesNotThrow { id }
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).getOrThrow()

    assertThat(testgrunnlag).isNotNull
    assertThat(testgrunnlag.namn).isEqualTo("Testgrunnlag")
    assertThat(testgrunnlag.testreglar).isEqualTo(listOf(1))
    assertThat(testgrunnlag.loeysing).isEqualTo(nyttTestgrunnlagManuell.loeysingar)
    assertThat(testgrunnlag.type).isEqualTo(Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST)
  }

  private fun opprettTestsak(): Sak {
    val sakId = sakDAO.save("TestSak", "111222333", java.time.LocalDate.now())
    val sak = sakDAO.getSak(sakId.getOrThrow()).getOrThrow()
    val sakTilOppdatering =
        sak.copy(
            loeysingar =
                listOf(
                    Sak.Loeysing(
                        1,
                        listOf(
                            Sak.Nettside(
                                1, "forside", "http://www.example.com", "forside", "forside")))),
            testreglar =
                listOf(
                    Testregel(
                        1,
                        "1.2.2a",
                        1,
                        "Testregelamn",
                        "1.2.2",
                        TestregelStatus.publisert,
                        Instant.now(),
                        TestregelInnholdstype.nett,
                        TestregelModus.manuell,
                        TestlabLocale.nb,
                        1,
                        1,
                        "Har ikkje feil",
                        "{}",
                        1)))

    sakDAO.update(sakTilOppdatering)
    return sakDAO.getSak(sakId.getOrThrow()).getOrThrow()
  }

  @Test
  fun testGetLoeysingar() {

    val loeysing: Sak.Loeysing =
        testSak?.loeysingar?.first() ?: throw IllegalStateException("TestSak har ingen loeysingar")

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            parentId = testSak?.id,
            namn = "Testgrunnlag",
            type = Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
            testregelar = listOf(1),
            loeysingar = loeysing)

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag)

    val expected = testSak?.loeysingar ?: emptyList()

    val actual = testgrunnlagDAO.getLoeysing(id.getOrThrow())

    assertThat(actual).isEqualTo(expected.first())
  }

  @Test
  fun testUpdateTestgrunnlag() {
    val loeysingar = testSak?.loeysingar ?: emptyList()

    val nyttTestgrunnlagManuell =
        NyttTestgrunnlag(
            parentId = testSak?.id,
            namn = "Testgrunnlag",
            type = Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
            testregelar = listOf(1),
            loeysingar = loeysingar.first())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)
    assertDoesNotThrow { id }
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).getOrThrow()

    val oppdatertTestgrunlag =
        testgrunnlag.copy(namn = "Oppdatert testgrunnlag", testreglar = listOf(1, 2))

    val result = testgrunnlagDAO.updateTestgrunnlag(oppdatertTestgrunlag)

    assertThat(result.getOrThrow()).isEqualTo(oppdatertTestgrunlag)
  }

  @Test
  fun testSlettTestgrunnlag() {
    val loeysingar = testSak?.loeysingar ?: emptyList()

    val nyttTestgrunnlagManuell =
        NyttTestgrunnlag(
            parentId = testSak?.id,
            namn = "Testgrunnlag",
            type = Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
            testregelar = listOf(1),
            loeysingar = loeysingar.first())

    val id = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlagManuell)

    testgrunnlagDAO.deleteTestgrunnlag(id.getOrThrow())

    assertThat(testgrunnlagDAO.getTestgrunnlag(id.getOrThrow()).isFailure).isTrue()

    assertThrows<IllegalArgumentException> { testgrunnlagDAO.getLoeysing(id.getOrThrow()) }
  }
}
