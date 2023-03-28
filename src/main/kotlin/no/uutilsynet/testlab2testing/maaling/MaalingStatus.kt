package no.uutilsynet.testlab2testing.maaling

enum class MaalingStatus(val status: String) {
  planlegging("planlegging"),
  crawling("crawling"),
  kvalitetssikring("kvalitetssikring"),
  testing("testing"),
  testing_ferdig("testing_ferdig")
}
