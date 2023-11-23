package no.uutilsynet.testlab2testing.forenkletkontroll

data class Framgang(val prosessert: Int, val maxLenker: Int) {
  companion object {
    fun from(customStatus: CustomStatus): Framgang =
        Framgang(customStatus.lenkerCrawla, customStatus.maxLenker)

    fun from(customStatus: AutoTesterClient.CustomStatus?, crawledPages: Int): Framgang =
        Framgang(customStatus?.testaSider ?: 0, customStatus?.talSider ?: crawledPages)
  }
}
