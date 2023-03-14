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

  private fun saveMaalingWithStatusKvalitetssikring(): Int {
    val id = maalingDAO.createMaaling("testmåling", loeysingList.map { it.id })
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
}
