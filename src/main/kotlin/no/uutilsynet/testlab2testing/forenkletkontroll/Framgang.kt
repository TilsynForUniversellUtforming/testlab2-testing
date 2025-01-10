package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.sideutval.crawling.CustomStatus
import no.uutilsynet.testlab2testing.testing.manuelltesting.AutoTesterClient

data class Framgang(val prosessert: Int, val maxLenker: Int) {
  companion object {
    fun from(customStatus: CustomStatus): Framgang =
        Framgang(customStatus.lenkerCrawla, customStatus.maxLenker)

    fun from(customStatus: AutoTesterClient.CustomStatus?, crawledPages: Int): Framgang =
        Framgang(customStatus?.testaSider ?: 0, customStatus?.talSider ?: crawledPages)
  }
}
