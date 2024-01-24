package no.uutilsynet.testlab2testing.regelsett

import java.time.*
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettName
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettStandard
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestregelIdList
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestregelList
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettType
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelModus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.annotation.CacheEvict

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegelsettDAOTest(
    @Autowired val regelsettDAO: RegelsettDAO,
    @Autowired val testregelDAO: TestregelDAO
) {

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
        Regelsett(id, regelsettName, regelsettType, regelsettStandard, regelsettTestregelList)

    assertThat(regelsett).isNotNull
    assertThat(regelsett).isEqualTo(expected)
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

  companion object {
    @JvmStatic
    fun regelsettBeforeUpdateList(): List<RegelsettCreate> =
        listOf(
            RegelsettCreate(
                "${regelsettName}_1", regelsettType, regelsettStandard, regelsettTestregelIdList),
            RegelsettCreate(regelsettName, regelsettType, true, regelsettTestregelIdList),
            RegelsettCreate(regelsettName, regelsettType, regelsettStandard, listOf(1)),
        )
  }

  @Test
  @DisplayName("Skal kunne oppdatere namn til regelsett")
  fun updateRegelsettName() {
    val nameBefore = "${regelsettName}_1"
    val id = createTestRegelsett(namn = nameBefore)
    val regelsettBefore = regelsettDAO.getRegelsett(id)

    val expectedBefore =
        Regelsett(id, nameBefore, regelsettType, regelsettStandard, regelsettTestregelList)

    assertThat(regelsettBefore).isNotNull
    assertThat(regelsettBefore).isEqualTo(expectedBefore)

    regelsettDAO.updateRegelsett(
        RegelsettEdit(
            id,
            regelsettName,
            regelsettType,
            regelsettStandard,
            regelsettTestregelIdList,
        ))

    val expectedAfter =
        Regelsett(id, regelsettName, regelsettType, regelsettStandard, regelsettTestregelList)
    val regelsettAfter = regelsettDAO.getRegelsett(id)

    assertThat(regelsettAfter).isNotNull
    assertThat(regelsettAfter).isEqualTo(expectedAfter)
  }

  @Test
  @DisplayName("Skal kunne oppdatere om regelsett er standard")
  fun updateRegelsettStandard() {
    val standardBefore = true
    val id = createTestRegelsett(standard = standardBefore)
    val regelsettBefore = regelsettDAO.getRegelsett(id)

    val expectedBefore =
        Regelsett(id, regelsettName, regelsettType, standardBefore, regelsettTestregelList)

    assertThat(regelsettBefore).isNotNull
    assertThat(regelsettBefore).isEqualTo(expectedBefore)

    regelsettDAO.updateRegelsett(
        RegelsettEdit(
            id,
            regelsettName,
            regelsettType,
            regelsettStandard,
            regelsettTestregelIdList,
        ))

    val expectedAfter =
        Regelsett(id, regelsettName, regelsettType, regelsettStandard, regelsettTestregelList)
    val regelsettAfter = regelsettDAO.getRegelsett(id)

    assertThat(regelsettAfter).isNotNull
    assertThat(regelsettAfter).isEqualTo(expectedAfter)
  }

  @Test
  @DisplayName("Skal kunne oppdatere testreglar i regelsett")
  fun updateRegelsettTestregel() {
    val testregelListBefore = listOf(regelsettTestregelList[0])
    val id = createTestRegelsett(testregelIdList = testregelListBefore.map { it.id })
    val regelsettBefore = regelsettDAO.getRegelsett(id)

    val expectedBefore =
        Regelsett(id, regelsettName, regelsettType, regelsettStandard, testregelListBefore)

    assertThat(regelsettBefore).isNotNull
    assertThat(regelsettBefore).isEqualTo(expectedBefore)

    regelsettDAO.updateRegelsett(
        RegelsettEdit(
            id,
            regelsettName,
            regelsettType,
            regelsettStandard,
            regelsettTestregelIdList,
        ))

    val expectedAfter =
        Regelsett(id, regelsettName, regelsettType, regelsettStandard, regelsettTestregelList)
    val regelsettAfter = regelsettDAO.getRegelsett(id)

    assertThat(regelsettAfter).isNotNull
    assertThat(regelsettAfter).isEqualTo(expectedAfter)
  }

  private fun createTestRegelsett(
      namn: String = regelsettName,
      type: TestregelModus = regelsettType,
      standard: Boolean = regelsettStandard,
      testregelIdList: List<Int> = regelsettTestregelIdList,
  ): Int {

    regelsettTestregelList.forEach { testregel -> testregelDAO.updateTestregel(testregel) }

    return regelsettDAO.createRegelsett(
        RegelsettCreate(
            namn,
            type,
            standard,
            testregelIdList,
        ))
  }
}
