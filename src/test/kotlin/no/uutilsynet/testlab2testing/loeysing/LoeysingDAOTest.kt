package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestName
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestUrl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoeysingDAOTest(@Autowired val loeysingDAO: LoeysingDAO) {

  @AfterAll
  fun cleanup() {
    loeysingDAO.jdbcTemplate.update(
        "delete from loeysing where namn = :namn", mapOf("namn" to loeysingTestName))
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

    oldLoeysing?.copy(namn = loeysingTestName, url = URL(loeysingTestUrl))?.let {
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

  private fun createLoeysing(name: String = loeysingTestName, url: String = loeysingTestUrl) =
      loeysingDAO.createLoeysing(name, URL(url))
}
