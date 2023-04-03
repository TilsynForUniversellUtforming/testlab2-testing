package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.maaling.TestConstants.loeysingList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@DisplayName("Tester for MaalingDAO")
@SpringBootTest
class MaalingDAOTest(@Autowired val maalingDAO: MaalingDAO) {

  @DisplayName(
      "når vi henter en måling fra databasen som har status 'kvalitetssikring', så skal crawlresultatene inneholde en liste med nettsider")
  @Test
  fun getMaaling() {
    val id = saveMaalingWithStatusKvalitetssikring()

    val maalingFromDatabase = maalingDAO.getMaaling(id) as Maaling.Kvalitetssikring

    assertThat(maalingFromDatabase.crawlResultat).isNotEmpty
    maalingFromDatabase.crawlResultat.forEach {
      val nettsider = (it as CrawlResultat.Ferdig).nettsider
      assertThat(nettsider)
          .contains(
              URL(it.loeysing.url, "/"),
              URL(it.loeysing.url, "/underside/1"),
              URL(it.loeysing.url, "/underside/2"))
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
      "når vi lagrer ei måling med status `testing_ferdig`, så skal alle resultatene også lagres")
  @Test
  fun lagreResultater() {
    val id = saveMaalingWithStatusTestingFerdig()
    val maaling = maalingDAO.getMaaling(id) as Maaling.TestingFerdig
    val testResultat = maaling.testKoeyringar.flatMap { it.testResultat }[0]
    assertThat(testResultat.testregelId).isEqualTo("QW-ACT-R5")
  }

  private fun saveMaalingWithStatusTestingFerdig(): Int {
    val id =
        maalingDAO.createMaaling("testing ferdig", loeysingList.map { it.id }, CrawlParameters())
    val maaling = maalingDAO.getMaaling(id) as Maaling.Planlegging
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              listOf(URL(it.url, "/"), URL(it.url, "/underside/1"), URL(it.url, "/underside/2")),
              URL("https://status.uri"),
              it,
              Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.Ferdig(
              it.loeysing,
              Instant.now(),
              URL("https://teststatus.url"),
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
    return id
  }

  private fun saveMaalingWithStatusKvalitetssikring(): Int {
    val id = maalingDAO.createMaaling("testmåling", loeysingList.map { it.id }, CrawlParameters())
    val maaling = maalingDAO.getMaaling(id) as Maaling.Planlegging
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              listOf(URL(it.url, "/"), URL(it.url, "/underside/1"), URL(it.url, "/underside/2")),
              URL("https://status.uri"),
              it,
              Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    maalingDAO.save(kvalitetssikring).getOrThrow()
    return id
  }

  private fun saveMaalingWithStatusTesting(): Int {
    val id = maalingDAO.createMaaling("testmåling", loeysingList.map { it.id }, CrawlParameters())
    val maaling = maalingDAO.getMaaling(id) as Maaling.Planlegging
    val crawlResultat =
        maaling.loeysingList.map {
          CrawlResultat.Ferdig(
              listOf(URL(it.url, "/"), URL(it.url, "/underside/1"), URL(it.url, "/underside/2")),
              URL("https://status.uri"),
              it,
              Instant.now())
        }
    val kvalitetssikring = Maaling.toKvalitetssikring(Maaling.toCrawling(maaling, crawlResultat))!!
    val testKoeyringar =
        crawlResultat.map {
          TestKoeyring.IkkjeStarta(it.loeysing, Instant.now(), URL("https://teststatus.url"))
        }
    val testing = Maaling.toTesting(kvalitetssikring, testKoeyringar)
    maalingDAO.save(testing).getOrThrow()
    return id
  }
}
