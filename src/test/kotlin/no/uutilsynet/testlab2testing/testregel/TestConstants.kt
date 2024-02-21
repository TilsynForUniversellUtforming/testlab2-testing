package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.common.TestlabLocale

object TestConstants {
  val name = "test_skal_slettes"
  val testregelTestKrav = "2.1.1 Tastatur"
  val testregelSchemaForenklet = "QW-ACT-R70"
  val testregelSchemaManuell = """{ "gaaTil": 1 }"""
  val modus = TestregelModus.forenklet

  val testregelCreateRequestBody =
      mapOf(
          "testregelId" to testregelSchemaForenklet,
          "namn" to name,
          "krav" to testregelTestKrav,
          "status" to TestregelStatus.publisert,
          "type" to TestregelInnholdstype.nett,
          "modus" to modus,
          "spraak" to TestlabLocale.nb,
          "testregelSchema" to testregelSchemaForenklet,
          "innhaldstypeTesting" to 1,
          "tema" to 1,
          "testobjekt" to 1,
          "kravTilSamsvar" to "",
      )
}
