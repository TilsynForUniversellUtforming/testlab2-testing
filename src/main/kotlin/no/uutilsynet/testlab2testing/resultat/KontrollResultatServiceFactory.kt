package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import org.springframework.stereotype.Service

@Service
class KontrollResultatServiceFactory(
    private val automatiskResultatService: AutomatiskResultatService,
    private val manueltResultatService: ManueltResultatService,
    private val kontrollDAO: KontrollDAO
) {

  fun getResultatService(kontrollId: Int): KontrollResultatService {
    return getResultatService(getTypeKontroll(kontrollId))
  }

  fun getResultatService(kontrollType: Kontrolltype): KontrollResultatService {
    return when (kontrollType) {
      Kontrolltype.ForenklaKontroll -> automatiskResultatService
      Kontrolltype.Statusmaaling,
      Kontrolltype.InngaaendeKontroll,
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak -> manueltResultatService
    }
  }

  private fun getTypeKontroll(kontrollId: Int): Kontrolltype {
    return kontrollDAO.getKontrollType(kontrollId)
  }
}
