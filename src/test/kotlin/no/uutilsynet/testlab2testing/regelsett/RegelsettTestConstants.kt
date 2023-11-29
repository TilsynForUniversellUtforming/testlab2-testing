package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelType

object RegelsettTestConstants {
  val regelsettName = "regelsett_slett"
  val regelsettType = TestregelType.forenklet
  val regelsettStandard = false

  val regelsettTestregelList =
      listOf(
          Testregel(
              1,
              "QW-ACT-R1 HTML Page has a title",
              "QW-ACT-R1",
              "2.4.2 Sidetitler",
              TestregelType.forenklet),
          Testregel(
              2,
              "QW-ACT-R2 HTML page has lang attribute",
              "QW-ACT-R2",
              "3.1.1 Språk på siden",
              TestregelType.forenklet))

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
