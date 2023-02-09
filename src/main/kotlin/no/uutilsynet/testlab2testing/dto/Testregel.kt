package no.uutilsynet.testlab2testing.dto

data class Testregel (
  override val id: Int,
  override val kravId: Int?,
  override val referanseAct: String?,
  override val kravTilSamsvar: String,
  override val type: String,
  override val status: String,
  val kravTittel: String?
) : TestregelDTO()