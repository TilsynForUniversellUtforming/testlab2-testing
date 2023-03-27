package no.uutilsynet.testlab2testing.maaling

data class CrawlParameters(val maxLinksPerPage: Int = 100, val numLinksToSelect: Int = 30) {
  companion object {
    fun CrawlParameters.validateParameters(): Unit =
        if (maxLinksPerPage > 2000) {
          throw IllegalArgumentException("Kan ikkje velgje meir enn 2000 sidar per løysing")
        } else if (numLinksToSelect > 2000) {
          throw IllegalArgumentException("Kan ikkje velgje større utval enn 2000 sidar per løysing")
        } else if (maxLinksPerPage < 10) {
          throw IllegalArgumentException("Kan ikkje velgje mindre enn 10 sidar per løysing")
        } else if (numLinksToSelect < 10) {
          throw IllegalArgumentException("Kan ikkje velgje mindre utval enn 10 sidar per løysing")
        } else Unit
  }
}
