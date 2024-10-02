package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettModus
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettName
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettStandard
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestregelIdList
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestregelList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegelsettDAOTest(@Autowired val regelsettDAO: RegelsettDAO) {

  @AfterAll
  fun cleanup() {
    regelsettDAO.jdbcTemplate.update(
        "delete from regelsett where namn = :namn", mapOf("namn" to regelsettName))
  }

  @Test
  @DisplayName("Skal kunne opprette et regelsett")
  fun createRegelsett() {
    val id = assertDoesNotThrow { createTestRegelsett() }
    val regelsett = regelsettDAO.getRegelsettResponse(id)
    assertThat(regelsett).isNotNull
  }

  @Test
  @DisplayName("Skal kunne hente regelsett")
  fun getRegelsett() {
    val id = createTestRegelsett()
    val regelsett = regelsettDAO.getRegelsett(id)
    val expected =
        Regelsett(id, regelsettName, regelsettModus, regelsettStandard, regelsettTestregelList)

    compareRegelsett(regelsett, expected)
  }

  @Test
  @DisplayName("Skal kunne hente liste med aktive regelsett")
  fun getRegelsettListActive() {
    val (id1, id2) = Pair(createTestRegelsett(), createTestRegelsett())
    val regelsettList = regelsettDAO.getRegelsettBaseList(false)

    assertThat(regelsettList).isNotEmpty
    val regelsettIdList = regelsettList.map { it.id }
    assertThat(regelsettIdList).contains(id1)
    assertThat(regelsettIdList).contains(id2)
  }

  @Test
  @DisplayName("Skal kunne hente liste med alle regelsett")
  fun getRegelsettListAll() {
    val (id1, id2) = Pair(createTestRegelsett(), createTestRegelsett())

    regelsettDAO.deleteRegelsett(id2)

    val regelsettList = regelsettDAO.getRegelsettBaseList(true)

    assertThat(regelsettList).isNotEmpty
    val regelsettIdList = regelsettList.map { it.id }
    assertThat(regelsettIdList).contains(id1)
    assertThat(regelsettIdList).contains(id2)
  }

  @Test
  @DisplayName("Skal kunne hente liste med aktive regelsett")
  fun getRegelsettListWithTestreglar() {
    val (id1, id2) = Pair(createTestRegelsett(), createTestRegelsett())
    val regelsettList = regelsettDAO.getRegelsettTestreglarList(false)

    assertThat(regelsettList).isNotEmpty

    val testregelIdList = listOf(1, 2)

    val regelsettTestregelIdMap =
        regelsettList.associate { tr -> tr.id to tr.testregelList.map { it.id } }
    assertThat(regelsettTestregelIdMap[id1]).containsExactlyInAnyOrderElementsOf(testregelIdList)
    assertThat(regelsettTestregelIdMap[id2]).containsExactlyInAnyOrderElementsOf(testregelIdList)

    val regelsettIdList = regelsettList.map { it.id }
    assertThat(regelsettIdList).contains(id1)
    assertThat(regelsettIdList).contains(id2)
  }

  @Test
  @DisplayName("Skal kunne slette (sette inaktivt) regelsett")
  fun deleteRegelsett() {
    val id = createTestRegelsett()
    val regelsettList = regelsettDAO.getRegelsettBaseList(false)
    assertThat(regelsettList.map { it.id }).contains(id)

    regelsettDAO.deleteRegelsett(id)

    val regelsettListAfterDelete = regelsettDAO.getRegelsettBaseList(false)
    assertThat(regelsettListAfterDelete.map { it.id }).doesNotContain(id)
  }

  @Test
  @DisplayName("Skal kunne oppdatere namn til regelsett")
  fun updateRegelsettName() {
    val nameBefore = "${regelsettName}_1"
    val id = createTestRegelsett(namn = nameBefore)
    val regelsettBefore = regelsettDAO.getRegelsett(id)

    val expectedBefore =
        Regelsett(id, nameBefore, regelsettModus, regelsettStandard, regelsettTestregelList)

    compareRegelsett(regelsettBefore, expectedBefore)

    regelsettDAO.updateRegelsett(
        RegelsettEdit(
            id,
            regelsettName,
            regelsettModus,
            regelsettStandard,
            regelsettTestregelIdList,
        ))

    val expectedAfter =
        Regelsett(id, regelsettName, regelsettModus, regelsettStandard, regelsettTestregelList)
    val regelsettAfter = regelsettDAO.getRegelsett(id)

    compareRegelsett(regelsettAfter, expectedAfter)
  }

  @Test
  @DisplayName("Skal kunne oppdatere om regelsett er standard")
  fun updateRegelsettStandard() {
    val standardBefore = true
    val id = createTestRegelsett(standard = standardBefore)
    val regelsettBefore = regelsettDAO.getRegelsett(id)

    val expectedBefore =
        Regelsett(id, regelsettName, regelsettModus, standardBefore, regelsettTestregelList)

    compareRegelsett(regelsettBefore, expectedBefore)

    regelsettDAO.updateRegelsett(
        RegelsettEdit(
            id,
            regelsettName,
            regelsettModus,
            regelsettStandard,
            regelsettTestregelIdList,
        ))

    val expectedAfter =
        Regelsett(id, regelsettName, regelsettModus, regelsettStandard, regelsettTestregelList)
    val regelsettAfter = regelsettDAO.getRegelsett(id)

    compareRegelsett(regelsettAfter, expectedAfter)
  }

  @Test
  @DisplayName("Skal kunne oppdatere testreglar i regelsett")
  fun updateRegelsettTestregel() {
    val testregelListBefore = listOf(regelsettTestregelList[0])
    val id = createTestRegelsett(testregelIdList = testregelListBefore.map { it.id })
    val regelsettBefore = regelsettDAO.getRegelsett(id)

    val expectedBefore =
        Regelsett(id, regelsettName, regelsettModus, regelsettStandard, testregelListBefore)

    compareRegelsett(regelsettBefore, expectedBefore)

    regelsettDAO.updateRegelsett(
        RegelsettEdit(
            id,
            regelsettName,
            regelsettModus,
            regelsettStandard,
            regelsettTestregelIdList,
        ))

    val expectedAfter =
        Regelsett(id, regelsettName, regelsettModus, regelsettStandard, regelsettTestregelList)
    val regelsettAfter = regelsettDAO.getRegelsett(id)

    compareRegelsett(regelsettAfter, expectedAfter)
  }

  private fun createTestRegelsett(
      namn: String = regelsettName,
      type: TestregelModus = regelsettModus,
      standard: Boolean = regelsettStandard,
      testregelIdList: List<Int> = regelsettTestregelIdList,
  ): Int =
      regelsettDAO.createRegelsett(
          RegelsettCreate(
              namn,
              type,
              standard,
              testregelIdList,
          ))

  private fun compareRegelsett(actual: Regelsett?, expected: Regelsett) {
    assertThat(actual).isNotNull
    assertThat(actual?.id).isEqualTo(expected.id)
    assertThat(actual?.namn).isEqualTo(expected.namn)
    assertThat(actual?.standard).isEqualTo(expected.standard)
    assertThat(actual?.modus).isEqualTo(expected.modus)
    assertThat(actual?.testregelList?.map { it.id }).isEqualTo(expected.testregelList.map { it.id })
  }
}
