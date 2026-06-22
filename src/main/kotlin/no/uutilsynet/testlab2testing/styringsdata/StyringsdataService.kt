package no.uutilsynet.testlab2testing.styringsdata

import org.springframework.stereotype.Service

@Service
class StyringsdataService(val styringsdataDAO: StyringsdataDAO) {

  fun getStyringsdataMapForKontroll(kontrollId: Int): Map<Int, StyringsdataListElement> =
      styringsdataDAO.getStyringsdataByKontrollId(kontrollId).associateBy { it.loeysingId }
}
