package no.uutilsynet.testlab2testing

fun tilfeldigOrgnummer(): String {
  val eightRandomDigits = (10000000..99999999).random()
  val weights = listOf(3, 2, 7, 6, 5, 4, 3, 2)
  val sum =
      eightRandomDigits
          .toString()
          .map { it.toString().toInt() }
          .zip(weights)
          .sumOf { (a, b) -> a * b }
  val checksum = 11 - (sum % 11)
  if (checksum == 10) {
    return tilfeldigOrgnummer()
  } else {
    val checksumDigit = if (checksum == 11) 0 else checksum
    return eightRandomDigits.toString() + checksumDigit.toString()
  }
}
