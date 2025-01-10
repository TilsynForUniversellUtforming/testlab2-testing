package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import org.springframework.stereotype.Service

@Service
class TestregelService(val testregelDAO: TestregelDAO, val kravregisterClient: KravregisterClient) {

  fun getKravWcag2x(testregelId: Int): KravWcag2x {
    val krav =
        getTestregel(testregelId)?.kravId?.let { kravregisterClient.getWcagKrav(it) }
            ?: throw RuntimeException("Fant ikke krav for testregel $testregelId")
    return krav
  }

  private fun getTestregel(testregelId: Int) = testregelDAO.getTestregel(testregelId)

  fun getSuksesskriteriumFromKrav(kravId: Int) =
      kravregisterClient.getSuksesskriteriumFromKrav(kravId)
}
