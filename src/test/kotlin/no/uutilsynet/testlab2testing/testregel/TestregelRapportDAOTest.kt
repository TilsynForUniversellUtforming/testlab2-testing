package no.uutilsynet.testlab2testing.testregel

import java.time.Instant
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaAutomatisk
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestregelRapportDAOTest(@Autowired val testregelDAO: TestregelDAO) {

  val deleteThese: MutableList<Int> = mutableListOf()

  @AfterAll
  fun cleanup() {
    deleteThese.forEach { testregelDAO.deleteTestregel(it) }
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
            testregelId = "QW-ACT-R1",
            namn = "test_skal_slettes_1",
            kravId = 1,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            spraak = TestlabLocale.nb,
            testregelSchema = "",
            innhaldstypeTesting = 1,
            tema = 1,
            testobjekt = 1,
            kravTilSamsvar = "")
    val id = createTestregel(testregelInit)

    val oldTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(oldTestregel).isNotNull
    Assertions.assertThat(oldTestregel?.kravId).isEqualTo(testregelInit.kravId)
    Assertions.assertThat(oldTestregel?.testregelSchema).isEqualTo(testregelInit.testregelSchema)
    Assertions.assertThat(oldTestregel?.namn).isEqualTo(testregelInit.namn)

    oldTestregel
        ?.copy(
            kravId = testregelTestKravId, testregelSchema = testregelSchemaAutomatisk, namn = name)
        ?.let { testregelDAO.updateTestregel(it) }

    val updatedTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(updatedTestregel).isNotNull
    Assertions.assertThat(updatedTestregel?.kravId).isEqualTo(testregelTestKravId)
    Assertions.assertThat(updatedTestregel?.testregelSchema).isEqualTo(testregelSchemaAutomatisk)
    Assertions.assertThat(updatedTestregel?.namn).isEqualTo(name)
  }

  @Test
  fun updateTestregelSetDatoSistEndra() {
    val testregelInit =
        TestregelInit(
            testregelId = "QW-ACT-R1",
            namn = "test_skal_slettes_1",
            kravId = 1,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            spraak = TestlabLocale.nb,
            datoSistEndra = Instant.now().minusSeconds(61),
            testregelSchema = "",
            innhaldstypeTesting = 1,
            tema = 1,
            testobjekt = 1,
            kravTilSamsvar = "")
    val id = createTestregel(testregelInit)

    val oldTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(oldTestregel).isNotNull
    Assertions.assertThat(oldTestregel?.kravId).isEqualTo(testregelInit.kravId)
    Assertions.assertThat(oldTestregel?.testregelSchema).isEqualTo(testregelInit.testregelSchema)
    Assertions.assertThat(oldTestregel?.namn).isEqualTo(testregelInit.namn)

    val oldDate = oldTestregel?.datoSistEndra

    oldTestregel
        ?.copy(
            kravId = testregelTestKravId, testregelSchema = testregelSchemaAutomatisk, namn = name)
        ?.let { testregelDAO.updateTestregel(it) }

    val updatedTestregel = testregelDAO.getTestregel(id)
    Assertions.assertThat(updatedTestregel).isNotNull
    Assertions.assertThat(updatedTestregel?.kravId).isEqualTo(testregelTestKravId)
    Assertions.assertThat(updatedTestregel?.testregelSchema).isEqualTo(testregelSchemaAutomatisk)
    Assertions.assertThat(updatedTestregel?.namn).isEqualTo(name)

    val newDate = updatedTestregel?.datoSistEndra

    Assertions.assertThat(newDate).isAfter(oldDate)
  }

  private fun createTestregel(
      testregelInit: TestregelInit =
          TestregelInit(
              testregelId = "QW-ACT-R1",
              namn = name,
              kravId = testregelTestKravId,
              status = TestregelStatus.publisert,
              type = TestregelInnholdstype.nett,
              modus = TestregelModus.automatisk,
              spraak = TestlabLocale.nb,
              testregelSchema = testregelSchemaAutomatisk,
              innhaldstypeTesting = 1,
              tema = 1,
              testobjekt = 1,
              kravTilSamsvar = "")
  ) = testregelDAO.createTestregel(testregelInit).also { deleteThese.add(it) }
}
