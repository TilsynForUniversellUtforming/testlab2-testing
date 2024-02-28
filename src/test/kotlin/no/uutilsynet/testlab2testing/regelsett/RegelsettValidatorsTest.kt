package no.uutilsynet.testlab2testing.regelsett

import java.time.Instant
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelInnholdstype
import no.uutilsynet.testlab2testing.testregel.TestregelModus
import no.uutilsynet.testlab2testing.testregel.TestregelStatus
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
            Testregel(
                1,
                "TR1",
                1,
                "QW-ACT-R12",
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R12 HTML Page has a title",
                "QW-ACT-R12",
                1),
            Testregel(
                2,
                "TR2",
                1,
                "QW-ACT-R13",
                2,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R13 HTML Page has a title",
                "QW-ACT-R13",
                1))
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelModus.forenklet

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isSuccess)
  }

  @Test
  @DisplayName("Skal validere regelsett med gyldige testreglar for manuell kontroll")
  fun validManuell() {
    val testregelList =
        listOf(
            Testregel(
                1,
                "TR1",
                1,
                "TR1",
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.manuell,
                TestlabLocale.nb,
                1,
                1,
                "1.1.1",
                """"{"gaaTil": 1}""",
                1),
            Testregel(
                2,
                "TR2",
                1,
                "TR2",
                2,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.manuell,
                TestlabLocale.nb,
                1,
                1,
                "1.1.1",
                """{"gaaTil": 1}""",
                1))
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelModus.manuell

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isSuccess)
  }

  @Test
  @DisplayName("Valderingen skal returnere riktige testreglar")
  fun validTestregelIdListLength() {
    val testregelList =
        listOf(
            Testregel(
                1,
                "TR1",
                1,
                "QW-ACT-R12",
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R12 HTML Page has a title",
                "QW-ACT-R12",
                1),
            Testregel(
                2,
                "TR2",
                1,
                "QW-ACT-R13",
                2,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R13 HTML Page has a title",
                "QW-ACT-R13",
                1),
            Testregel(
                3,
                "TR3",
                1,
                "QW-ACT-R14",
                3,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R14 HTML Page has a title",
                "QW-ACT-R14",
                1),
        )
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelModus.forenklet

    val validation = validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList)

    Assertions.assertThat(validation.getOrThrow()).isEqualTo(testregelIdList)
  }

  @Test
  @DisplayName("Skal feile validering for regelsett med både manuell og forenklede testregler")
  fun invalidBothManuellForenklet() {
    val testregelList =
        listOf(
            Testregel(
                1,
                "TR1",
                1,
                "QW-ACT-R12",
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R12 HTML Page has a title",
                "QW-ACT-R12",
                1),
            Testregel(
                2,
                "TR2",
                1,
                "QW-ACT-R13",
                2,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.manuell,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R13 HTML Page has a title",
                "QW-ACT-R13",
                1))
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelModus.forenklet

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isFailure)
  }

  @Test
  @DisplayName("Skal feile validering for regelsett som inneholder ikke-eksisterende testregler")
  fun invalidNonExistingTestregel() {
    val testregelList =
        listOf(
            Testregel(
                1,
                "TR1",
                1,
                "QW-ACT-R12",
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.forenklet,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R12 HTML Page has a title",
                "QW-ACT-R12",
                1),
        )
    val testregelIdList = listOf(1, 2)
    val regelsettType = TestregelModus.forenklet

    assertTrue(validateRegelsettTestreglar(testregelIdList, regelsettType, testregelList).isFailure)
  }
}
