package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.Random
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestName
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestOrgNummer
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestUrl
import no.uutilsynet.testlab2testing.maaling.CrawlParameters
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import no.uutilsynet.testlab2testing.maaling.MaalingListElement
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingTestName
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoeysingDAOTest(
    @Autowired val loeysingDAO: LoeysingDAO,
    @Autowired val maalingDAO: MaalingDAO
) {

  @AfterEach
  fun cleanup() {
    loeysingDAO.jdbcTemplate.update(
        "delete from loeysing where namn = :namn", mapOf("namn" to loeysingTestName))
    maalingDAO.jdbcTemplate.update(
        "delete from maalingv1 where navn = :navn", mapOf("navn" to maalingTestName))
  }

  @Test
  @DisplayName("Skal hente løsning fra DAO")
  fun getLoeysing() {
    val id = createLoeysing()
    val loeysing = loeysingDAO.getLoeysing(id)
    Assertions.assertThat(loeysing).isNotNull
    Assertions.assertThat(loeysing?.namn).isEqualTo(loeysingTestName)
    Assertions.assertThat(loeysing?.url?.toString()).isEqualTo(loeysingTestUrl)
  }

  @Test
  @DisplayName("Skal hente løsningliste fra DAO")
  fun getLoeysingList() {
    val id = createLoeysing()
    val loeysing = loeysingDAO.getLoeysing(id)
    val list = loeysingDAO.getLoeysingList()

    Assertions.assertThat(list).contains(loeysing)
  }

  @Test
  @DisplayName("Skal hente løsningliste fra DAO basert på delvis navn")
  fun getLoeysingListByName() {
    val id = createLoeysing()
    val loeysing = loeysingDAO.getLoeysing(id)
    val list = loeysingDAO.findByName(loeysingTestName.substring(1, 7))

    Assertions.assertThat(list).containsExactly(loeysing)
  }

  @Test
  @DisplayName("Skal hente løsningliste fra DAO basert på delvis orgnr")
  fun getLoeysingListByOrgnr() {
    val id = createLoeysing()
    val loeysing = loeysingDAO.getLoeysing(id)
    val list = loeysingDAO.findByOrgnumber(loeysingTestOrgNummer.substring(1, 7))

    Assertions.assertThat(list).containsExactly(loeysing)
  }

  @Test
  @DisplayName("Skal opprette løsning i DAO")
  fun insertLoeysing() {
    val id = assertDoesNotThrow { createLoeysing() }
    val loeysing = loeysingDAO.getLoeysing(id)
    Assertions.assertThat(loeysing).isNotNull
  }

  @Test
  @DisplayName("Skal slette løsning i DAO")
  fun deleteLoeysing() {
    val id = createLoeysing()
    val existingLoeysing = loeysingDAO.getLoeysing(id)
    Assertions.assertThat(existingLoeysing).isNotNull

    loeysingDAO.deleteLoeysing(id)

    val nonExistingLoeysing = loeysingDAO.getLoeysing(id)
    Assertions.assertThat(nonExistingLoeysing).isNull()
  }

  @Test
  @DisplayName("Skal oppdatere løsning i DAO")
  fun updateLoeysing() {
    val oldName = "test_skal_slettes_1"
    val oldUrl = "https://www.w3.org"
    val id = createLoeysing(oldName, oldUrl)

    val oldLoeysing = loeysingDAO.getLoeysing(id)
    Assertions.assertThat(oldLoeysing).isNotNull
    Assertions.assertThat(oldLoeysing?.namn).isEqualTo(oldName)
    Assertions.assertThat(oldLoeysing?.url?.toString()).isEqualTo(oldUrl)

    oldLoeysing?.copy(namn = loeysingTestName, url = URI(loeysingTestUrl).toURL())?.let {
      loeysingDAO.updateLoeysing(it)
    }

    val updatedLoeysing = loeysingDAO.getLoeysing(id)
    Assertions.assertThat(updatedLoeysing).isNotNull
    Assertions.assertThat(updatedLoeysing?.namn).isEqualTo(loeysingTestName)
    Assertions.assertThat(updatedLoeysing?.url?.toString()).isEqualTo(loeysingTestUrl)
  }

  @Nested
  @DisplayName("getLoeysingIdList")
  inner class GetLoeysingIdList {
    @Test
    @DisplayName(
        "når vi henter id-lista, så skal den inneholde alle id-ene på løsninger i databasen")
    fun getIdList() {
      val id1 = createLoeysing()
      val id2 = createLoeysing()
      val id3 = createLoeysing()
      val idList = loeysingDAO.getLoeysingIdList()
      Assertions.assertThat(idList).contains(id1, id2, id3)
    }
  }

  @DisplayName("findLoeysingListForMaaling")
  @Nested
  inner class FindLoeysingListForMaaling {
    @DisplayName(
        "når det finnes ei måling med to løysingar, så skal vi kunne hente lista med løysingar for den målinga")
    @Test
    fun listeMedLoeysingar() {
      val a = createLoeysing(loeysingTestName, "https://www.a.com/")
      val b = createLoeysing(loeysingTestName, "https://www.b.com/")
      val maaling =
          maalingDAO.createMaaling(
              maalingTestName, LocalDate.now(), listOf(a, b), emptyList(), CrawlParameters())

      val loeysingList = loeysingDAO.findLoeysingListForMaaling(maaling)

      Assertions.assertThat(loeysingList.map(Loeysing::url).map(URL::toString))
          .contains("https://www.a.com/", "https://www.b.com/")
    }

    @DisplayName("når målinga ikkje finnes, så skal den returnere ei tom liste")
    @Test
    fun maalingaFinnesIkkje() {
      val idList = maalingDAO.getMaalingList().map(MaalingListElement::id)
      val id = getNumberNotInList(idList)

      val loeysingList = loeysingDAO.findLoeysingListForMaaling(id)

      Assertions.assertThat(loeysingList).isEmpty()
    }

    private fun getNumberNotInList(list: List<Int>): Int {
      val random = Random()
      var i = random.nextInt()

      while (i in list) {
        i = random.nextInt()
      }

      return i
    }
  }

  private fun createLoeysing(name: String = loeysingTestName, url: String = loeysingTestUrl) =
      loeysingDAO.createLoeysingInternal(name, URI(url).toURL(), loeysingTestOrgNummer)
}
