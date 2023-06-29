package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.maaling.TestConstants.digdirLoeysing
import no.uutilsynet.testlab2testing.maaling.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingTestName
import no.uutilsynet.testlab2testing.maaling.TestConstants.testRegelList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@DisplayName("Tester for MaalingDAO")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaalingDAOTest(@Autowired val maalingDAO: MaalingDAO) {

  @AfterAll
  fun cleanup() {
    maalingDAO.jdbcTemplate.update(
        "delete from maalingv1 where navn = :navn", mapOf("navn" to maalingTestName))
  }

  @DisplayName(
      "når vi henter en måling fra databasen som har status 'kvalitetssikring', så skal crawlresultatene inneholde en liste med nettsider")
  @Test
  fun getMaaling() {
    val id = saveMaalingWithStatusKvalitetssikring()

    val maalingFromDatabase = maalingDAO.getMaaling(id) as Maaling.Kvalitetssikring

    assertThat(maalingFromDatabase.crawlResultat).isNotEmpty
    maalingFromDatabase.crawlResultat.forEach {
      val nettsider = (it as CrawlResultat.Ferdig).nettsider
      assertThat(nettsider).containsAll(it.loeysing.url.toUrlListWithPages())
    }
  }

  @DisplayName(
      "når vi lagrer ei måling med status `crawling`, og crawlresultatene ikkje er ferdige, så skal framgangen også lagrast")
  @Test
  fun lagreCrawlResultatMedFramgang() {
    val id = saveMaalingWithStatusCrawling()
    val maaling = maalingDAO.getMaaling(id) as Maaling.Crawling
    maaling.crawlResultat.forEach {
      val (lenkerCrawla, maxLenker) = (it as CrawlResultat.IkkeFerdig).framgang
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
      "når vi lagrer ei måling med status `testing_ferdig` og testresultater, så skal alle resultatene også lagres")
  @Test
  fun lagreResultater() {
    val maaling = createTestMaaling()
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              it.url.toUrlListWithPages(), URI("https://status.uri").toURL(), it, Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.Ferdig(
              it,
              Instant.now(),
              URI("https://teststatus.url").toURL(),
              listOf(
                  TestResultat(
                      listOf("3.1.1"),
                      it.loeysing.url,
                      "QW-ACT-R5",
                      1,
                      TestResultat.parseLocalDateTime("3/23/2023, 11:15:54 AM"),
                      "The `lang` attribute has a valid value.",
                      "samsvar",
                      TestResultat.ACTElement(
                          "html",
                          "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT53aW5kb3cuZGF0YQ=="))))
        }
    val testing = Maaling.toTesting(kvalitetssikring, testKoeyringar)
    val testingFerdig = Maaling.toTestingFerdig(testing)!!
    maalingDAO.save(testingFerdig).getOrThrow()
    val id = maaling.id
    val maalingFraDatabasen = maalingDAO.getMaaling(id) as Maaling.TestingFerdig
    val testResultat =
        maalingFraDatabasen.testKoeyringar
            .filterIsInstance<TestKoeyring.Ferdig>()
            .flatMap { it.testResultat }
            .first()
    assertThat(testResultat.testregelId).isEqualTo("QW-ACT-R5")
  }

  @DisplayName(
      "når vi lagrer ei måling med status `testing_ferdig` og med lenker til testresultatene, så skal lenkene også lagres")
  @Test
  fun lagreLenker() {
    val id = saveMaalingWithStatusTestingFerdig()

    val maalingFromDatabase = maalingDAO.getMaaling(id) as Maaling.TestingFerdig

    val testKoeyring = maalingFromDatabase.testKoeyringar[0] as TestKoeyring.Ferdig
    assertThat(testKoeyring.testResultat).isEmpty()
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

  private fun saveMaalingWithStatusTestingFerdig(): Int {
    val maaling = createTestMaaling()
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              it.url.toUrlListWithPages(), URI("https://status.uri").toURL(), it, Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.Ferdig(
              it,
              Instant.now(),
              URI("https://teststatus.url").toURL(),
              emptyList(),
              AutoTesterClient.AutoTesterOutput.Lenker(
                  URL("https://fullt.resultat"),
                  URL("https://brot.resultat"),
                  URL("https://aggregering.resultat")))
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
              maalingDateStart,
              loeysingList.map { it.id },
              testRegelList.map { it.id },
              CrawlParameters())
          .let { maalingDAO.getMaaling(it) as Maaling.Planlegging }

  private fun saveMaalingWithStatusKvalitetssikring(name: String = maalingTestName): Int {
    val maaling = createTestMaaling(name)
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              it.url.toUrlListWithPages(), URI("https://status.uri").toURL(), it, Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    maalingDAO.save(kvalitetssikring).getOrThrow()
    return maaling.id
  }

  private fun saveMaalingWithStatusCrawling(name: String = maalingTestName): Int {
    val maxLinksPerPage = 2000
    val maaling =
        maalingDAO
            .createMaaling(
                name,
                maalingDateStart,
                loeysingList.map { it.id },
                testRegelList.map { it.id },
                CrawlParameters(maxLinksPerPage, 30))
            .let { maalingDAO.getMaaling(it) as Maaling.Planlegging }
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.IkkeFerdig(
              URI("https://status.uri").toURL(), it, Instant.now(), Framgang(10, maxLinksPerPage))
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
              it.url.toUrlListWithPages(), URI("https://status.uri").toURL(), it, Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
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
              it.url.toUrlListWithPages(), URI("https://status.uri").toURL(), it, Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
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

  private fun URL.toUrlListWithPages() =
      listOf(URL(this, "/"), URL(this, "/underside/1"), URL(this, "/underside/2"))
}
