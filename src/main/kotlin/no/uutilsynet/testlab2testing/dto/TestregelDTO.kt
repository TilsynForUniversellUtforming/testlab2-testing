package no.uutilsynet.testlab2testing.dto

abstract class TestregelDTO{
  abstract val id: Int
  abstract val kravId: Int?
  abstract val referanseAct: String?
  abstract val kravTilSamsvar: String
  abstract val type: String
  abstract val status: String
}

