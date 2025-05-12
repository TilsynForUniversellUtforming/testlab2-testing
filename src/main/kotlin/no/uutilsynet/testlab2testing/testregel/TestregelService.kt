package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import org.springframework.stereotype.Service

@Service
class TestregelService(val testregelDAO: TestregelDAO, val kravregisterClient: KravregisterClient) {

  fun getKravWcag2x(testregelId: Int): KravWcag2x {
    val krav = getTestregel(testregelId).kravId.let { kravregisterClient.getWcagKrav(it) }
    return krav
  }

  fun getTestregel(testregelId: Int): Testregel =
      testregelDAO.getTestregel(testregelId)
          ?: throw IllegalArgumentException("Fant ikkje testregel med id $testregelId")
}
