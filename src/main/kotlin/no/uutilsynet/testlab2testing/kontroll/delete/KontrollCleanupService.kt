package no.uutilsynet.testlab2testing.kontroll.delete

import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDeleteDAO
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataCleanupDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatCleanupDAO
import org.springframework.stereotype.Component

@Component
class KontrollCleanupService(
  private val kontrollDeleteDAO: KontrollDeleteDAO,
  private val testresultatCleanupDAO: TestresultatCleanupDAO,
  private val styringsdataCleanupDAO: StyringsdataCleanupDAO,
  private val testgrunnlagDeleteDAO: TestgrunnlagDeleteDAO,
  private var testgrunnlagDAO: TestgrunnlagDAO
) {

  fun cleanupKontrollData(kontrollId: Int) {
    styringsdataCleanupDAO.deleteStyringdataLoeysingByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringsdataLoeysingPaaleggByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringsdataBotByKontrollId(kontrollId)
    styringsdataCleanupDAO.deleteStyringsdatLoeysingKlageByKontrollId(kontrollId)
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
    kontrollDeleteDAO.deleteKontrollByKontrollId(kontrollId)
  }

  fun cleanupTestgrunnlagData(testgrunnlagId: Int) {
    testresultatCleanupDAO.deleteAggregeringSuksesskriteriumByTestgrunnlagId(testgrunnlagId)
    testresultatCleanupDAO.deleteAggregeringTestregelByTestgrunnlagId(testgrunnlagId)
    testresultatCleanupDAO.deleteAggregeringSideByTestgrunnlagId(testgrunnlagId)
    testresultatCleanupDAO.deleteTestresultatByTestgrunnlagId(testgrunnlagId)

    testgrunnlagDeleteDAO.deleteTestgrunnlagLoeysingByTestgrunnlagId(testgrunnlagId)
    testgrunnlagDeleteDAO.deleteTestgrunnlagSideutvalKontrollByTestgrunnlagId(testgrunnlagId)
    testgrunnlagDeleteDAO.deleteTestgrunnlagTestregelByTestgrunnlagId(testgrunnlagId)
    testgrunnlagDAO.deleteTestgrunnlag(testgrunnlagId)

  }
}
