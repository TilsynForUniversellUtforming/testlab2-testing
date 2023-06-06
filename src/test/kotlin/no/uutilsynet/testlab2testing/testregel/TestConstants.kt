package no.uutilsynet.testlab2testing.testregel

object TestConstants {
  val testregelTestKrav = "2.1.1 Tastatur"
  val testregelTestReferanseAct = "QW-ACT-R70"
  val testregelTestKravtilsamsvar = "test_skal_slettes"

  val testregelRequestBody =
      mapOf(
          "krav" to testregelTestKrav,
          "referanseAct" to testregelTestReferanseAct,
          "kravTilSamsvar" to testregelTestKravtilsamsvar)
}
