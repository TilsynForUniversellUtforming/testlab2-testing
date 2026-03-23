package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.resultat.ResultatMetadataService
import org.springframework.stereotype.Service

@Service
class KontrollResultatServiceFactory(
  private val automatiskResultatService: AutomatiskResultatService,
  private val manueltResultatService: ManueltResultatService,
  private val kontrollDAO: KontrollDAO,
  private val dbAggregatedResults: DBAggregatedResults,
  private val resutatAppResultatService: ResultatAppResultatService,
  private val resultatMetadataService: ResultatMetadataService
) {

  fun getResultatService(kontrollId: Int): KontrollResultatService {
    return if(resultsInDB(kontrollId)) {
      getResultatService(getTypeKontroll(kontrollId))
    }
    else
      resutatAppResultatService
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

  private fun resultsInDB(kontrollId: Int): Boolean {
    return resultatMetadataService.hentResultatMetadata(kontrollId, null).isNotEmpty()
  }

  fun getAggregatedResultatService(kontrollId: Int): AggregatedResultsInterface {
    return dbAggregatedResults
  }

  private fun getTypeKontroll(kontrollId: Int): Kontrolltype {
    return kontrollDAO.getKontrollType(kontrollId)
  }

  private fun getAggregationServiceType(kontrollId: Int) {
    val testregelList: KontrollDAO.KontrollDB.Testreglar? = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first().testreglar
  }
}
