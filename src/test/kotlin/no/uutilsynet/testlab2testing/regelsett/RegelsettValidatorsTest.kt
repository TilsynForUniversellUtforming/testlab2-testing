package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RegelsettValidatorsTest {

  @Test
  @DisplayName("Skal validere regelsett med gyldige testreglar for forenklet kontroll")
  fun validForenklet() {
    val testregelList =
        listOf(
            Testregel(1, "TR1", "QW-ACT-R12", "1.1.1", TestregelType.forenklet),
            Testregel(2, "TR2", "QW-ACT-R13", "1.1.1", TestregelType.forenklet))
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelType.forenklet

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isSuccess)
  }

  @Test
  @DisplayName("Skal validere regelsett med gyldige testreglar for manuell kontroll")
  fun validManuell() {
    val testregelList =
        listOf(
            Testregel(1, "TR1", """{"gaaTil": 1}""", "1.1.1", TestregelType.manuell),
            Testregel(2, "TR2", """{"gaaTil": 1}""", "1.1.1", TestregelType.manuell))
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelType.manuell

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isSuccess)
  }

  @Test
  @DisplayName("Valderingen skal returnere riktige testreglar")
  fun validTestregelIdListLength() {
    val testregelList =
        listOf(
            Testregel(1, "TR1", "QW-ACT-R12", "1.1.1", TestregelType.forenklet),
            Testregel(2, "TR2", "QW-ACT-R13", "1.1.1", TestregelType.forenklet),
            Testregel(3, "TR3", "QW-ACT-R14", "1.1.1", TestregelType.forenklet),
        )
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelType.forenklet

    val validation = validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList)

    Assertions.assertThat(validation.getOrThrow()).isEqualTo(testregelIdList)
  }

  @Test
  @DisplayName("Skal feile validering for regelsett med både manuell og forenklede testregler")
  fun invalidBothManuellForenklet() {
    val testregelList =
        listOf(
            Testregel(1, "TR1", "QW-ACT-R12", "1.1.1", TestregelType.forenklet),
            Testregel(2, "TR2", "QW-ACT-R13", "1.1.2", TestregelType.manuell))
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelType.forenklet

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isFailure)
  }

  @Test
  @DisplayName("Skal feile validering for regelsett som inneholder ikke-eksisterende testregler")
  fun invalidNonExistingTestregel() {
    val testregelList =
        listOf(
            Testregel(1, "TR1", "QW-ACT-R12", "1.1.1", TestregelType.forenklet),
        )
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelType.forenklet

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isFailure)
  }
}
