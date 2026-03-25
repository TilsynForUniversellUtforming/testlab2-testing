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
  private val resultatMetadataService: ResultatMetadataService,
  private val externalAggregatedResultsService: ExternalAggregatedResultsService
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
    return resultatMetadataService.harResultInDb(kontrollId)
  }

  fun getAggregatedResultatService(kontrollId: Int): AggregatedResultsInterface {
    val results = resultsInDB(kontrollId)
    println("KontrollId: $kontrollId, Results in DB: $results")
    if(resultsInDB(kontrollId)) {
      return dbAggregatedResults
    }
    return externalAggregatedResultsService
  }

  private fun getTypeKontroll(kontrollId: Int): Kontrolltype {
    return kontrollDAO.getKontrollType(kontrollId)
  }
}
