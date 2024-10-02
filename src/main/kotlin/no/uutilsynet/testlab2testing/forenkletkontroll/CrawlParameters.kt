package no.uutilsynet.testlab2testing.forenkletkontroll

data class CrawlParameters(val maxLenker: Int = 100, val talLenker: Int = 30) {
  companion object {
    fun CrawlParameters.validateParameters(): Unit =
        when {
          maxLenker > 10000 -> {
            throw IllegalArgumentException("Kan ikkje velgje meir enn 10000 sidar per løysing")
          }
          talLenker > 2000 -> {
            throw IllegalArgumentException(
                "Kan ikkje velgje større utval enn 2000 sidar per løysing")
          }
          maxLenker < 1 -> {
            throw IllegalArgumentException("Kan ikkje velgje mindre enn 1 sidar per løysing")
          }
          talLenker < 1 -> {
            throw IllegalArgumentException("Kan ikkje velgje mindre utval enn 1 sidar per løysing")
          }
          else -> Unit
        }
  }
}
