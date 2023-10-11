package no.uutilsynet.testlab2testing.maaling

data class CrawlParameters(val maxLenker: Int = 100, val talLenker: Int = 30) {
  companion object {
    fun CrawlParameters.validateParameters(): Unit =
        if (maxLenker > 10000) {
          throw IllegalArgumentException("Kan ikkje velgje meir enn 10000 sidar per løysing")
        } else if (talLenker > 2000) {
          throw IllegalArgumentException("Kan ikkje velgje større utval enn 2000 sidar per løysing")
        } else if (maxLenker < 1) {
          throw IllegalArgumentException("Kan ikkje velgje mindre enn 1 sidar per løysing")
        } else if (talLenker < 1) {
          throw IllegalArgumentException("Kan ikkje velgje mindre utval enn 1 sidar per løysing")
        } else Unit
  }
}
