package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.digdirLoeysing
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingTestName
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.testRegelList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.uutilsynetLoeysing
import no.uutilsynet.testlab2testing.forenkletkontroll.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@DisplayName("Tester for MaalingDAO")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaalingDAOTest(
    @Autowired val maalingDAO: MaalingDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val crawlresultatDAO: CrawlresultatDAO
) {

  @MockBean lateinit var aggregeringService: AggregeringService

  @BeforeAll
  fun beforeAll() {
    loeysingList.forEach { loeysingsRegisterClient.saveLoeysing(it.namn, it.url, it.orgnummer) }
  }

  val deleteTheseIds: MutableSet<Int> = mutableSetOf()

  @AfterAll
  fun cleanup() {
    maalingDAO.jdbcTemplate.update(
        "delete from maalingv1 where navn = :navn", mapOf("navn" to maalingTestName))

    if (deleteTheseIds.isNotEmpty()) {
      maalingDAO.jdbcTemplate.update(
          "delete from maalingv1 where id in (:ids)", mapOf("ids" to deleteTheseIds))
    }
  }

  @DisplayName(
      "når vi lagar ei ny måling med ei løysing, og slettar løysinga etterpå, så skal vi fortsatt få data om løysinga når vi henter målinga")
  @Test
  fun getMaalingMedSlettaLoeysing() {
    val loeysing =
        loeysingsRegisterClient
            .saveLoeysing(
                "Løysing som skal bli sletta",
                URI("https://www.snartsletta.no/").toURL(),
                "123456785")
            .getOrThrow()

    val maalingId =
        maalingDAO
            .createMaaling(
                "måling med sletta løysing",
                Instant.now(),
                listOf(loeysing.id),
                testRegelList.map(Testregel::id),
                CrawlParameters())
            .also { id -> deleteTheseIds.add(id) }

    loeysingsRegisterClient.delete(loeysing.id).getOrThrow()

    val maaling = maalingDAO.getMaaling(maalingId) as Maaling.Planlegging

    assertThat(maaling.loeysingList).containsExactly(loeysing)
  }

  @DisplayName(
      "når vi henter en måling fra databasen som har status 'kvalitetssikring', så skal crawlresultatene inneholde en liste med nettsider")
  @Test
  fun getMaaling() {
    val id = saveMaalingWithStatusKvalitetssikring()

    val maalingFromDatabase = maalingDAO.getMaaling(id) as Maaling.Kvalitetssikring

    assertThat(maalingFromDatabase.crawlResultat).isNotEmpty
    maalingFromDatabase.crawlResultat.forEach {
      val antallNettsider = (it as CrawlResultat.Ferdig).antallNettsider
      assertThat(antallNettsider).isEqualTo(3)
    }
  }

  @DisplayName(
      "når vi lagrer ei måling med status `crawling`, og crawlresultatene ikkje er ferdige, så skal framgangen også lagrast")
  @Test
  fun lagreCrawlResultatMedFramgang() {
    val id = saveMaalingWithStatusCrawling()
    val maaling = maalingDAO.getMaaling(id) as Maaling.Crawling
    maaling.crawlResultat.forEach {
      val (lenkerCrawla, maxLenker) = (it as CrawlResultat.Starta).framgang
      assertThat(lenkerCrawla).isEqualTo(10)
      assertThat(maxLenker).isEqualTo(2000)
    }
  }

  @DisplayName(
      "når vi lagrer ei måling med status `testing`, så skal alle testkøyringene også lagrast")
  @Test
  fun lagreTestkoeyringar() {
    val id = saveMaalingWithStatusTesting()
    val maaling = maalingDAO.getMaaling(id) as Maaling.Testing
    assertThat(maaling.testKoeyringar).isNotEmpty
  }

  @DisplayName(
      "Måling med med status `testing` og testkøyring med status `starta` skal ha framgang med antall sider lik antall sider crawlet")
  @Test
  fun lagreTestkoeyringarStarta() {
    val id = saveMaalingWithStatusTestingStarta()
    val maaling = maalingDAO.getMaaling(id) as Maaling.Testing
    assertThat(maaling.testKoeyringar).isNotEmpty
    val testKoeyringStarta = assertDoesNotThrow { maaling.testKoeyringar[0] as TestKoeyring.Starta }
    val (prossesert, antall) = testKoeyringStarta.framgang
    assertThat(prossesert).isEqualTo(0)
    assertThat(antall).isEqualTo(loeysingList[0].url.toUrlListWithPages().size)
  }

  @DisplayName(
      "når vi har lagra målingar med status 'crawling' og 'testing', så skal vi kunne finne dei når vi søker på status")
  @Test
  fun getMaalingByStatus() {
    val idCrawling = saveMaalingWithStatusCrawling()
    val idTesting = saveMaalingWithStatusTesting()
    val maalingar =
        maalingDAO.getMaalingListByStatus(listOf(MaalingStatus.crawling, MaalingStatus.testing))
    assertThat(maalingar.map { it.id }).contains(idCrawling, idTesting)
  }

  @DisplayName(
      "når vi lagrer ei måling med status `testing_ferdig` og med lenker til testresultatene, så skal lenkene også lagres")
  @Test
  fun lagreLenker() {
    val id = saveMaalingWithStatusTestingFerdig()

    val maalingFromDatabase = maalingDAO.getMaaling(id) as Maaling.TestingFerdig

    val testKoeyring = maalingFromDatabase.testKoeyringar[0] as TestKoeyring.Ferdig
    assertThat(testKoeyring.lenker?.urlFulltResultat)
        .isEqualTo(URI("https://fullt.resultat").toURL())
    assertThat(testKoeyring.lenker?.urlBrot).isEqualTo(URI("https://brot.resultat").toURL())
  }

  @DisplayName("Skal kunne oppdatere måling i Planlegging")
  @Test
  fun updateMaalingPlanlegging() {
    val maalingNavnOriginal = "TestMåling"
    val maalingNavn = maalingTestName
    val crawlParameters = CrawlParameters(10, 10)
    val loeysingList = listOf(digdirLoeysing)

    val maaling: Maaling.Planlegging = createTestMaaling(maalingNavnOriginal)
    maalingDAO.updateMaaling(
        maaling.copy(
            navn = maalingTestName, crawlParameters = crawlParameters, loeysingList = loeysingList))
    val updatedMaaling = maalingDAO.getMaaling(maaling.id) as Maaling.Planlegging

    assertThat(updatedMaaling.navn).isEqualTo(maalingNavn)
    assertThat(updatedMaaling.crawlParameters).isEqualTo(crawlParameters)
    assertThat(updatedMaaling.loeysingList).containsExactly(digdirLoeysing)
  }

  @DisplayName("Skal kunne oppdatere måling i annen status")
  @Test
  fun updateMaalingOtherStatus() {
    val maalingNavnOriginal = "TestMåling"
    val maalingNavn = maalingTestName

    val id = saveMaalingWithStatusKvalitetssikring(maalingNavnOriginal)
    val maaling = maalingDAO.getMaaling(id) as Maaling.Kvalitetssikring
    assertThat(maaling.navn).isEqualTo(maalingNavnOriginal)

    maalingDAO.updateMaaling(maaling.copy(navn = maalingTestName))
    val updatedMaaling = maalingDAO.getMaaling(maaling.id) as Maaling.Kvalitetssikring

    assertThat(updatedMaaling.navn).isEqualTo(maalingNavn)
  }

  @DisplayName("Skal kunne slette måling")
  @Test
  fun deleteMaaling() {
    val maaling = createTestMaaling()
    val deletedRows = maalingDAO.deleteMaaling(maaling.id)
    assertThat(deletedRows).isEqualTo(1)
    val nonExistingMaaling = maalingDAO.getMaaling(maaling.id)
    assertThat(nonExistingMaaling).isNull()
  }

  @DisplayName("Skal kunne hente crawlresultat med riktig antall nettsider for måling")
  @Test
  fun getCrawlResultatForMaaling() {
    val maaling = createTestMaaling()
    val digdirUrls = digdirLoeysing.url.toUrlListWithPages(5)
    val uutilsynetUrls = uutilsynetLoeysing.url.toUrlListWithPages(50)

    val kvalitetssikring =
        Maaling.toKvalitetssikring(
            Maaling.toCrawling(
                maaling,
                listOf(
                    CrawlResultat.Ferdig(
                        digdirUrls.size,
                        URI("https://status.uri").toURL(),
                        digdirLoeysing,
                        Instant.now(),
                        digdirUrls,
                    ),
                    CrawlResultat.Ferdig(
                        uutilsynetUrls.size,
                        URI("https://status.uri").toURL(),
                        uutilsynetLoeysing,
                        Instant.now(),
                        uutilsynetUrls),
                )))!!
    maalingDAO.save(kvalitetssikring).getOrThrow()
    val crawlResults =
        crawlresultatDAO.getCrawlResultatForMaaling(
            maaling.id, listOf(digdirLoeysing, uutilsynetLoeysing))
    val digdirCrawlResult = crawlResults.find { it.loeysing == digdirLoeysing }
    assertThat(digdirCrawlResult).isInstanceOf(CrawlResultat.Ferdig::class.java)
    assertThat((digdirCrawlResult as CrawlResultat.Ferdig).antallNettsider)
        .isEqualTo(digdirUrls.size)

    val uuTilsynetCrawlResult = crawlResults.find { it.loeysing == uutilsynetLoeysing }
    assertThat(uuTilsynetCrawlResult).isInstanceOf(CrawlResultat.Ferdig::class.java)
    assertThat((uuTilsynetCrawlResult as CrawlResultat.Ferdig).antallNettsider)
        .isEqualTo(uutilsynetUrls.size)
  }

  @DisplayName("Skal ikke ikke oppdatere CrawlResultat.Ferdig mer enn en gang")
  @Test
  fun shouldNotInsertFinishedCrawlResultTwice() {
    val maaling = createTestMaaling()

    val crawling =
        Maaling.toCrawling(
            maaling,
            listOf(
                CrawlResultat.Ferdig(
                    5,
                    URI("https://status.uri").toURL(),
                    digdirLoeysing,
                    Instant.now(),
                )))

    maalingDAO.save(crawling).getOrThrow()
    val crId =
        maalingDAO.jdbcTemplate.queryForObject(
            "select id from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
            mapOf("loeysingid" to digdirLoeysing.id, "maaling_id" to maaling.id),
            Int::class.java)
    maalingDAO.save(crawling).getOrThrow()

    val crIdAfterInsert =
        maalingDAO.jdbcTemplate.queryForObject(
            "select id from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
            mapOf("loeysingid" to digdirLoeysing.id, "maaling_id" to maaling.id),
            Int::class.java)

    assertThat(crId).isEqualTo(crIdAfterInsert)
  }

  @DisplayName(
      "Skal kunne oppdatere CrawlResultat med andre statuser enn CrawlResultat.Ferdig mer enn en gang")
  @Test
  fun shouldInsertNonFinishedCrawlResultTwice() {
    val maaling = createTestMaaling()

    val crawling =
        Maaling.toCrawling(
            maaling,
            listOf(
                CrawlResultat.Starta(
                    URI("https://status.uri").toURL(),
                    digdirLoeysing,
                    Instant.now(),
                    Framgang(0, 10))))

    maalingDAO.save(crawling).getOrThrow()
    val crId =
        maalingDAO.jdbcTemplate.queryForObject(
            "select id from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
            mapOf("loeysingid" to digdirLoeysing.id, "maaling_id" to maaling.id),
            Int::class.java)
    maalingDAO.save(crawling).getOrThrow()

    val crIdAfterInsert =
        maalingDAO.jdbcTemplate.queryForObject(
            "select id from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
            mapOf("loeysingid" to digdirLoeysing.id, "maaling_id" to maaling.id),
            Int::class.java)

    assertThat(crId).isNotEqualTo(crIdAfterInsert)
  }

  private fun saveMaalingWithStatusTestingFerdig(): Int {
    val maaling = createTestMaaling()
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              1, URI("https://status.uri").toURL(), it, Instant.now(), it.url.toUrlListWithPages())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    maalingDAO.save(kvalitetssikring)
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.Ferdig(
              it,
              Instant.now(),
              URI("https://teststatus.url").toURL(),
              AutoTesterClient.AutoTesterLenker(
                  URI("https://fullt.resultat").toURL(),
                  URI("https://brot.resultat").toURL(),
                  URI("https://aggregering.resultat").toURL(),
                  URI("https://aggregeringSK.resultat").toURL(),
                  URI("https://aggregeringSide.resultat").toURL(),
                  URI("https://aggregeringSideTR.resultat").toURL(),
                  URI("https://aggregeringLoeysing.resultat").toURL()))
        }
    val testing = Maaling.toTesting(kvalitetssikring, testKoeyringar)
    val testingFerdig = Maaling.toTestingFerdig(testing)!!
    maalingDAO.save(testingFerdig).getOrThrow()
    return maaling.id
  }

  private fun createTestMaaling(name: String = maalingTestName): Maaling.Planlegging =
      maalingDAO
          .createMaaling(
              name,
              Instant.now(),
              loeysingList.map { it.id },
              testRegelList.map { it.id },
              CrawlParameters())
          .let { maalingDAO.getMaaling(it) as Maaling.Planlegging }

  private fun saveMaalingWithStatusKvalitetssikring(name: String = maalingTestName): Int {
    val maaling = createTestMaaling(name)
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              1, URI("https://status.uri").toURL(), it, Instant.now(), it.url.toUrlListWithPages())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    maalingDAO.save(kvalitetssikring).getOrThrow()
    return maaling.id
  }

  private fun saveMaalingWithStatusCrawling(name: String = maalingTestName): Int {
    val maxLenker = 2000
    val maaling =
        maalingDAO
            .createMaaling(
                name,
                maalingDateStart,
                loeysingList.map { it.id },
                testRegelList.map { it.id },
                CrawlParameters(maxLenker, 30))
            .let { maalingDAO.getMaaling(it) as Maaling.Planlegging }
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Starta(
              URI("https://status.uri").toURL(), it, Instant.now(), Framgang(10, maxLenker))
        }
    val crawling = Maaling.toCrawling(maaling, crawlResultat)
    maalingDAO.save(crawling).getOrThrow()
    return maaling.id
  }

  private fun saveMaalingWithStatusTesting(): Int {
    val maaling = createTestMaaling()
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              1, URI("https://status.uri").toURL(), it, Instant.now(), it.url.toUrlListWithPages())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    maalingDAO.save(kvalitetssikring)
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.IkkjeStarta(it, Instant.now(), URI("https://teststatus.url").toURL())
        }
    val testing = Maaling.toTesting(kvalitetssikring, testKoeyringar)
    maalingDAO.save(testing).getOrThrow()
    return maaling.id
  }

  private fun saveMaalingWithStatusTestingStarta(): Int {
    val maaling = createTestMaaling()
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              1, URI("https://status.uri").toURL(), it, Instant.now(), it.url.toUrlListWithPages())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    maalingDAO.save(kvalitetssikring)
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.Starta(
              it,
              Instant.now(),
              URI("https://teststatus.url").toURL(),
              Framgang(0, it.nettsider.size))
        }
    val testing = Maaling.toTesting(kvalitetssikring, testKoeyringar)
    maalingDAO.save(testing).getOrThrow()
    return maaling.id
  }

  private fun URL.toUrlListWithPages(numberOfPages: Int = 3) =
      (1..numberOfPages).map { URI("${this}/underside/$it").toURL() }
}
