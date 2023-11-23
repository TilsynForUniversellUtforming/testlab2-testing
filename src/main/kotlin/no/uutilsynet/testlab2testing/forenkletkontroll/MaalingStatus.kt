package no.uutilsynet.testlab2testing.forenkletkontroll

enum class MaalingStatus(val status: String) {
  planlegging("planlegging"),
  crawling("crawling"),
  kvalitetssikring("kvalitetssikring"),
  testing("testing"),
  testing_ferdig("testing_ferdig")
}
