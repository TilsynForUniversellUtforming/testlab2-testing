package no.uutilsynet.testlab2testing.testregel

object TestConstants {
  val name = "test_skal_slettes"
  val testregelTestKrav = "2.1.1 Tastatur"
  val testregelSchemaForenklet = "QW-ACT-R70"
  val testregelSchemaManuell = """{ "gaaTil": 1 }"""
  val type = TestregelType.forenklet

  val testregelRequestBody =
      mapOf(
          "name" to name,
          "krav" to testregelTestKrav,
          "testregelSchema" to testregelSchemaForenklet,
          "type" to type,
      )
}
