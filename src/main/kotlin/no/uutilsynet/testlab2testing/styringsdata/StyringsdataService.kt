package no.uutilsynet.testlab2testing.styringsdata

import org.springframework.stereotype.Service

@Service
class StyringsdataService(val styringsdataDAO: StyringsdataDAO) {

  fun getStyringsdataForLoeysing(loeysingId: Int, kontrollId: Int): StyringsdataListElement? {
    return styringsdataDAO.getStyringsdataLoeysing(kontrollId).singleOrNull {
      it.loeysingId == loeysingId
    }
  }

  fun getStyringsdataMapForKontroll(kontrollId: Int): Map<Int, StyringsdataListElement> =
      styringsdataDAO.getStyringsdataByKontrollId(kontrollId).associateBy { it.loeysingId }
}
