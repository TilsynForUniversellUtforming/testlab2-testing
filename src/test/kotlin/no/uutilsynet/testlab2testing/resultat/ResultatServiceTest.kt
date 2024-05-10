package no.uutilsynet.testlab2testing.resultat

import java.time.Instant
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.testregel.*
import org.apache.commons.lang3.RandomStringUtils
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
    @Autowired val maalingDao: MaalingDAO
) {

  @Test
  fun getTestresultatMaaling() {
    val testMaaling = createTestMaaling()

    val resultat =
        resultatService.getResultatList(Kontroll.Kontrolltype.ForenklaKontroll).filter {
          it.id == testMaaling
        }

    assert(resultat.isNotEmpty())

    val resultatMaaling = resultat[0]
    assertEquals(resultatMaaling.id, testMaaling)
    assertEquals(resultatMaaling.namn, "Testmaaling_resultat")
    assertEquals(resultatMaaling.loeysingar.size, 1)
    assertEquals(resultatMaaling.loeysingar[0].score, 0.5)
  }

  fun createTestMaaling(): Int {
    val crawlParameters = CrawlParameters(10, 10)
    val testregelNoekkel = RandomStringUtils.randomAlphanumeric(5)

    val testregel =
        TestregelInit(
            testregelId = testregelNoekkel,
            namn = TestConstants.name,
            kravId = 1,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            spraak = TestlabLocale.nb,
            testregelSchema = testregelNoekkel,
            innhaldstypeTesting = 1,
            tema = 1,
            testobjekt = 1,
            kravTilSamsvar = "")

    val testregelId = testregelDAO.createTestregel(testregel)

    val maalingId =
        maalingDao.createMaaling(
            "Testmaaling_resultat", Instant.now(), listOf(1), listOf(testregelId), crawlParameters)

    val testresultat: AggregeringPerTestregelDTO =
        AggregeringPerTestregelDTO(
            maalingId, 1, testregelId, 1, arrayListOf(1, 2), 1, 2, 1, 1, 1, 1, 1, 0.5f, 0.5f, null)

    aggregeringDAO.createAggregertResultatTestregel(testresultat)

    return maalingId
  }
}
