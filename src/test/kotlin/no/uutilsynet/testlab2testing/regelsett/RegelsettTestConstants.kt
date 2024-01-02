package no.uutilsynet.testlab2testing.regelsett

import java.time.LocalDate
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelInnholdstype
import no.uutilsynet.testlab2testing.testregel.TestregelStatus
import no.uutilsynet.testlab2testing.testregel.TestregelType

object RegelsettTestConstants {
  val regelsettName = "regelsett_slett"
  val regelsettType = TestregelType.forenklet
  val regelsettStandard = false

  val regelsettTestregelList =
      listOf(
          Testregel(
              1,
              "QW-ACT-R1",
              1,
              "QW-ACT-R1 HTML Page has a title",
              "2.4.2",
              TestregelStatus.publisert,
              LocalDate.now(),
              TestregelInnholdstype.nett,
              TestregelType.forenklet,
              TestlabLocale.nb,
              1,
              1,
              "QW-ACT-R1 HTML Page has a title",
              "QW-ACT-R1"),
          Testregel(
              2,
              "QW-ACT-R2",
              1,
              "QW-ACT-R2 HTML page has lang attribute",
              "3.1.1",
              TestregelStatus.publisert,
              LocalDate.now(),
              TestregelInnholdstype.nett,
              TestregelType.forenklet,
              TestlabLocale.nb,
              1,
              1,
              "Språk på siden",
              "QW-ACT-R2"))

  val regelsettTestregelIdList = regelsettTestregelList.map { it.id }

  fun regelsettTestCreateRequestBody(
      namn: String = regelsettName,
      type: TestregelType = regelsettType,
      standard: Boolean = regelsettStandard,
      testregelIdList: List<Int> = regelsettTestregelIdList,
  ) =
      mapOf(
          "namn" to namn,
          "type" to type,
          "standard" to standard,
          "testregelIdList" to testregelIdList)
}
