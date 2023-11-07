package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.util.*
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestName
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestOrgNummer
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestUrl
import no.uutilsynet.testlab2testing.maaling.CrawlParameters
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import no.uutilsynet.testlab2testing.maaling.MaalingListElement
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingTestName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper

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
  @DisplayName("Skal opprette løsning i DAO")
  fun insertLoeysing() {
    val id = assertDoesNotThrow { createLoeysing() }
    assertThat(id).isNotNull()
  }

  @Test
  @DisplayName("Skal slette løsning i DAO")
  fun deleteLoeysing() {
    val id = createLoeysing()
    val existingLoeysing = getLoeysing(id)
    assertThat(existingLoeysing).isNotNull

    loeysingDAO.deleteLoeysing(id)

    val nonExistingLoeysing = getLoeysing(id)
    assertThat(nonExistingLoeysing).isNull()
  }

  @Test
  @DisplayName("Skal oppdatere løsning i DAO")
  fun updateLoeysing() {
    val oldName = "test_skal_slettes_1"
    val oldUrl = "https://www.w3.org"
    val id = createLoeysing(oldName, oldUrl)

    val oldLoeysing = getLoeysing(id)
    assertThat(oldLoeysing).isNotNull
    assertThat(oldLoeysing?.namn).isEqualTo(oldName)
    assertThat(oldLoeysing?.url?.toString()).isEqualTo(oldUrl)

    oldLoeysing?.copy(namn = loeysingTestName, url = URI(loeysingTestUrl).toURL())?.let {
      loeysingDAO.updateLoeysing(it)
    }

    val updatedLoeysing = getLoeysing(id)
    assertThat(updatedLoeysing).isNotNull
    assertThat(updatedLoeysing?.namn).isEqualTo(loeysingTestName)
    assertThat(updatedLoeysing?.url?.toString()).isEqualTo(loeysingTestUrl)
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

      assertThat(loeysingList.map(Loeysing::url).map(URL::toString))
          .contains("https://www.a.com/", "https://www.b.com/")
    }

    @DisplayName("når målinga ikkje finnes, så skal den returnere ei tom liste")
    @Test
    fun maalingaFinnesIkkje() {
      val idList = maalingDAO.getMaalingList().map(MaalingListElement::id)
      val id = getNumberNotInList(idList)

      val loeysingList = loeysingDAO.findLoeysingListForMaaling(id)

      assertThat(loeysingList).isEmpty()
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

  private fun getLoeysing(id: Int): Loeysing? =
      DataAccessUtils.singleResult(
          loeysingDAO.jdbcTemplate.query(
              "select id, namn, url, orgnummer from loeysing where id = :id",
              mapOf("id" to id),
              DataClassRowMapper.newInstance(Loeysing::class.java)))
}
