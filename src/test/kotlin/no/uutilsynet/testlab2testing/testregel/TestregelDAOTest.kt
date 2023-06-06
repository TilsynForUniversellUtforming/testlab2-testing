package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravtilsamsvar
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestReferanseAct
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
        mapOf("kravtilsamsvar" to testregelTestKravtilsamsvar))
  }

  @Test
  @DisplayName("Skal hente testregel fra DAO")
  fun getTestregel() {
    val id = createTestregel()
    val testregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(testregel).isNotNull
    Assertions.assertThat(testregel?.kravTilSamsvar).isEqualTo(testregelTestKravtilsamsvar)
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
    val oldReferanseAct = "QW-ACT-R69"
    val oldKravtilsamsvar = "test_skal_slettes_1"
    val id =
        createTestregel(
            oldKrav,
            oldReferanseAct,
            oldKravtilsamsvar,
        )

    val oldTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(oldTestregel).isNotNull
    Assertions.assertThat(oldTestregel?.krav).isEqualTo(oldKrav)
    Assertions.assertThat(oldTestregel?.referanseAct).isEqualTo(oldReferanseAct)
    Assertions.assertThat(oldTestregel?.kravTilSamsvar).isEqualTo(oldKravtilsamsvar)

    oldTestregel
        ?.copy(
            krav = testregelTestReferanseAct,
            referanseAct = testregelTestKrav,
            kravTilSamsvar = testregelTestKravtilsamsvar)
        ?.let { testregelDAO.updateTestregel(it) }

    val updatedTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(updatedTestregel).isNotNull
    Assertions.assertThat(updatedTestregel?.krav).isEqualTo(testregelTestReferanseAct)
    Assertions.assertThat(updatedTestregel?.referanseAct).isEqualTo(testregelTestKrav)
    Assertions.assertThat(updatedTestregel?.kravTilSamsvar).isEqualTo(testregelTestKravtilsamsvar)
  }

  private fun createTestregel(
      krav: String = testregelTestReferanseAct,
      referanseAct: String = testregelTestKrav,
      kravtilsamsvar: String = testregelTestKravtilsamsvar,
  ) = testregelDAO.createTestregel(krav, referanseAct, kravtilsamsvar)
}
