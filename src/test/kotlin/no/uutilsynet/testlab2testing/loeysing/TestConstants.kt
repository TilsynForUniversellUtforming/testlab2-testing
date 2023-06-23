package no.uutilsynet.testlab2testing.loeysing

object TestConstants {
  val loeysingTestName = "test_skal_slettes"
  val loeysingTestUrl = "https://www.example.com"
  val loeysingTestOrgNummer = "123456785"

  val loeysingRequestBody =
      mapOf(
          "namn" to loeysingTestName,
          "url" to loeysingTestUrl,
          "orgnummer" to loeysingTestOrgNummer)
}
