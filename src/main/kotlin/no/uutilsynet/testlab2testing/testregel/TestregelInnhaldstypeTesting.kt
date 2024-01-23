package no.uutilsynet.testlab2testing.testregel

enum class TestregelInnhaldstypeTesting(val value: String) {
  bilde_grafikk("Bilde og grafikk"),
  captcha("Captcha"),
  heile_nettsida("Heile nettsida"),
  iframe("Iframe"),
  innhald_tidavgrensing("Innhald med tidsavgrensing"),
  innhald_blinkar("Innhald som blinkar og/eller oppdaterer automatisk"),
  kjeldekode("Kjeldekode"),
  lenke("Lenke"),
  liste("Liste"),
  lyd_bilde("Lyd og video"),
  overskrift("Overskrift"),
  skjema("Skjema"),
  skjermelement("Skjemaelement"),
  statusmelding("Statusmelding"),
  sveiping("Sveiping"),
  tabell("Tabell"),
  tastatur("Tastatur"),
  tekst("Tekst"),
}
