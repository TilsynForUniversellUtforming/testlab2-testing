package no.uutilsynet.testlab2testing.regelsett

import java.time.Instant
import java.time.temporal.ChronoUnit
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.Testregel

object RegelsettTestConstants {
  val regelsettName = "regelsett_slett"
  val regelsettModus = TestregelModus.automatisk
  val regelsettStandard = false

  val regelsettTestregelList =
      listOf(
          Testregel(
              1,
              "QW-ACT-R1",
              1,
              "QW-ACT-R1 HTML Page has a title",
              1,
              TestregelStatus.publisert,
              Instant.now().truncatedTo(ChronoUnit.MINUTES),
              TestregelInnholdstype.nett,
              TestregelModus.automatisk,
              TestlabLocale.nb,
              1,
              1,
              "HTML Page has a title",
              "QW-ACT-R1",
              1),
          Testregel(
              2,
              "QW-ACT-R2",
              1,
              "QW-ACT-R2 HTML page has lang attribute",
              2,
              TestregelStatus.publisert,
              Instant.now().truncatedTo(ChronoUnit.MINUTES),
              TestregelInnholdstype.nett,
              TestregelModus.automatisk,
              TestlabLocale.nb,
              1,
              1,
              "Språk på siden",
              "QW-ACT-R2",
              1))

  val regelsettTestregelIdList = regelsettTestregelList.map { it.id }

  fun regelsettTestCreateRequestBody(
      namn: String = regelsettName,
      modus: TestregelModus = regelsettModus,
      standard: Boolean = regelsettStandard,
      testregelIdList: List<Int> = regelsettTestregelIdList,
  ) =
      mapOf(
          "namn" to namn,
          "modus" to modus,
          "standard" to standard,
          "testregelIdList" to testregelIdList)
}
