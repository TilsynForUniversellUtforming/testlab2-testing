package no.uutilsynet.testlab2testing.common

fun validateOrgNummer(s: String?): Result<String> = runCatching {
  requireNotNull(s) { "Organisasjonsnummer kan ikkje vere null" }
  require(s.all { it.isDigit() }) { "Organisasjonsnummer kan berre innehalde siffer" }
  require(s.length == 9) { "Organisasjonsnummer må vere 9 siffer" }

  val orgnummer = s.toCharArray().map { it.toString().toInt() }
  val vekter = listOf(3, 2, 7, 6, 5, 4, 3, 2)
  val sum = orgnummer.take(8).zip(vekter).sumOf { (a, b) -> a * b }
  val rest = sum % 11
  val kontrollsiffer = if (rest == 0) 0 else 11 - rest
  if (kontrollsiffer == orgnummer[8]) {
    s
  } else {
    throw IllegalArgumentException("$s er ikkje eit gyldig organisasjonsnummer")
  }
}
