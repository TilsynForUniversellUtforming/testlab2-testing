package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.TestlabLocale

object TestConstants {
  val name = "test_skal_slettes"
  val testregelTestKravId = 1
  val testregelSchemaAutomatisk = "QW-ACT-R70"
  val testregelSchemaManuell = """{ "gaaTil": 1 }"""
  val modus = TestregelModus.automatisk

  val testregelCreateRequestBody =
      mapOf(
          "testregelId" to testregelSchemaAutomatisk,
          "namn" to name,
          "kravId" to testregelTestKravId,
          "status" to TestregelStatus.publisert,
          "type" to TestregelInnholdstype.nett,
          "modus" to modus,
          "spraak" to TestlabLocale.nb,
          "testregelSchema" to testregelSchemaAutomatisk,
          "innhaldstypeTesting" to 1,
          "tema" to 1,
          "testobjekt" to 1,
          "kravTilSamsvar" to "",
      )
}
