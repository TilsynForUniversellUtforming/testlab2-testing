package no.uutilsynet.testlab2testing.testregel

object TestConstants {
  val testregelTestKrav = "2.1.1 Tastatur"
  val testregelTestTestregelNoekkel = "QW-ACT-R70"
  val testregelTestKravTilSamsvar = "test_skal_slettes"

  val testregelRequestBody =
      mapOf(
          "krav" to testregelTestKrav,
          "testregelNoekkel" to testregelTestTestregelNoekkel,
          "kravTilSamsvar" to testregelTestKravTilSamsvar)
}
