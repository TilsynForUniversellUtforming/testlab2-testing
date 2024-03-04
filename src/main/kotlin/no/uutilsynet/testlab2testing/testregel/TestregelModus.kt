package no.uutilsynet.testlab2testing.testregel

enum class TestregelModus(val value: String) {
  automatisk("automatisk"),
  semiAutomatisk("semi-automatisk"),
  manuell("manuell"),
  forenklet("forenklet")
}
