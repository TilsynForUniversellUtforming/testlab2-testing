package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravTilSamsvar
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestTestregelNoekkel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestregelDAOTest(@Autowired val testregelDAO: TestregelDAO) {

  @AfterAll
  fun cleanup() {
    testregelDAO.jdbcTemplate.update(
        "delete from testregel where kravtilsamsvar = :kravtilsamsvar",
        mapOf("kravtilsamsvar" to testregelTestKravTilSamsvar))
  }

  @Test
  @DisplayName("Skal hente testregel fra DAO")
  fun getTestregel() {
    val id = createTestregel()
    val testregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(testregel).isNotNull
    Assertions.assertThat(testregel?.kravTilSamsvar).isEqualTo(testregelTestKravTilSamsvar)
  }

  @Test
  @DisplayName("Skal hente testregelliste fra DAO")
  fun getTestregelList() {
    val id = createTestregel()
    val testregel = testregelDAO.getTestregel(id)
    val list = testregelDAO.getTestregelList()

    Assertions.assertThat(list).contains(testregel)
  }

  @Test
  @DisplayName("Skal opprette testregel i DAO")
  fun insertTestregel() {
    val id = assertDoesNotThrow { createTestregel() }
    val testregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(testregel).isNotNull
  }

  @Test
  @DisplayName("Skal slette testregel i DAO")
  fun deleteTestregel() {
    val id = createTestregel()
    val existingTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(existingTestregel).isNotNull

    testregelDAO.deleteTestregel(id)

    val nonExistingTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(nonExistingTestregel).isNull()
  }

  @Test
  @DisplayName("Skal oppdatere testregel i DAO")
  fun updateTestregel() {
    val oldKrav = "1.4.12 Tekstavstand"
    val oldTestregelNoekkel = "QW-ACT-R69"
    val oldKravtilsamsvar = "test_skal_slettes_1"
    val id =
        createTestregel(
            oldKrav,
            oldTestregelNoekkel,
            oldKravtilsamsvar,
        )

    val oldTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(oldTestregel).isNotNull
    Assertions.assertThat(oldTestregel?.krav).isEqualTo(oldKrav)
    Assertions.assertThat(oldTestregel?.testregelNoekkel).isEqualTo(oldTestregelNoekkel)
    Assertions.assertThat(oldTestregel?.kravTilSamsvar).isEqualTo(oldKravtilsamsvar)

    oldTestregel
        ?.copy(
            krav = testregelTestKrav,
            testregelNoekkel = testregelTestTestregelNoekkel,
            kravTilSamsvar = testregelTestKravTilSamsvar)
        ?.let { testregelDAO.updateTestregel(it) }

    val updatedTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(updatedTestregel).isNotNull
    Assertions.assertThat(updatedTestregel?.krav).isEqualTo(testregelTestKrav)
    Assertions.assertThat(updatedTestregel?.testregelNoekkel)
        .isEqualTo(testregelTestTestregelNoekkel)
    Assertions.assertThat(updatedTestregel?.kravTilSamsvar).isEqualTo(testregelTestKravTilSamsvar)
  }

  private fun createTestregel(
      krav: String = testregelTestKrav,
      testregelNoekkel: String = testregelTestTestregelNoekkel,
      kravtilsamsvar: String = testregelTestKravTilSamsvar,
  ) = testregelDAO.createTestregel(krav, testregelNoekkel, kravtilsamsvar)
}
