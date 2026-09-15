package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2testing.styringsdata.StyringsdataCleanupDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatCleanupDAO
import org.springframework.stereotype.Component

@Component
class KontrollCleanupService(
    private val kontrollDeleteDAO: KontrollDeleteDAO,
    private val testresultatCleanupDAO: TestresultatCleanupDAO,
    private val styringsdataCleanupDAO: StyringsdataCleanupDAO
) {

  fun cleanupKontrollData(kontrollId: Int) {

    styringsdataCleanupDAO.deleteStyringsdataLoeysingPaaleggByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringsdataBotByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringsdatLoeysingKlageByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringdataLoeysingByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringdataKontrollByKontrollId(kontrollId)

    // Delete data in the correct order to avoid foreign key constraint violations
    testresultatCleanupDAO.deleteAggregeringSuksesskriteriumByKontrollId(kontrollId)
    testresultatCleanupDAO.deleteAggregeringTestregelByKontrollId(kontrollId)
    testresultatCleanupDAO.deleteAggregeringSideByKontrollId(kontrollId)
    testresultatCleanupDAO.deleteTestresultatByKontrollId(kontrollId)

    kontrollDeleteDAO.deleteTestgrunnlagLoeysingByKontrollId(kontrollId)
    kontrollDeleteDAO.deleteTestgrunnlagSideutvalKontrollByKontrollId(kontrollId)
    kontrollDeleteDAO.deleteTestgrunnlagTestregelByKontrollId(kontrollId)
    kontrollDeleteDAO.deleteTestgrunnlagByKontrollId(kontrollId)

    kontrollDeleteDAO.deleteKontrollLoeysingByKontrollId(kontrollId)
    kontrollDeleteDAO.deleteKontrollTestregelByKontrollId(kontrollId)
    kontrollDeleteDAO.deleteKontrollSideutvalByKontrollId(kontrollId)
  }
}
