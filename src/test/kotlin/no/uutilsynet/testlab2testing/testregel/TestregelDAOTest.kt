package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaForenklet
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
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
        "delete from testregel where namn = :name", mapOf("name" to name))
  }

  @Test
  @DisplayName("Skal hente testregel fra DAO")
  fun getTestregel() {
    val id = createTestregel()
    val testregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(testregel).isNotNull
    Assertions.assertThat(testregel?.namn).isEqualTo(name)
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
    val testregelInit =
        TestregelInit(
            "test_skal_slettes_1", "QW-ACT-R69", "1.4.12 Tekstavstand", TestregelType.forenklet)
    val id = createTestregel(testregelInit)

    val oldTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(oldTestregel).isNotNull
    Assertions.assertThat(oldTestregel?.krav).isEqualTo(testregelInit.krav)
    Assertions.assertThat(oldTestregel?.testregelSchema).isEqualTo(testregelInit.testregelSchema)
    Assertions.assertThat(oldTestregel?.namn).isEqualTo(testregelInit.name)

    oldTestregel
        ?.copy(krav = testregelTestKrav, testregelSchema = testregelSchemaForenklet, namn = name)
        ?.let { testregelDAO.updateTestregel(it) }

    val updatedTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(updatedTestregel).isNotNull
    Assertions.assertThat(updatedTestregel?.krav).isEqualTo(testregelTestKrav)
    Assertions.assertThat(updatedTestregel?.testregelSchema).isEqualTo(testregelSchemaForenklet)
    Assertions.assertThat(updatedTestregel?.namn).isEqualTo(name)
  }

  private fun createTestregel(
      testregelInit: TestregelInit =
          TestregelInit(
              name,
              testregelTestKrav,
              testregelSchemaForenklet,
              TestregelType.forenklet,
          )
  ) = testregelDAO.createTestregel(testregelInit)
}
