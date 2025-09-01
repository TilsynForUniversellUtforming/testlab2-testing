package no.uutilsynet.testlab2testing.testregel

import java.time.Instant
import no.uutilsynet.testlab2.constants.*
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.import.TestregelImportService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.Mockito.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestregelResourceTest {

  private val kravregisterClient = mock<KravregisterClient>()
  private val testregelImportService = mock<TestregelImportService>()
  private val maalingService = mock<MaalingService>()
  private val testregelService = mock<TestregelService>()

  private val resource =
      TestregelResource(
          kravregisterClient, testregelImportService, maalingService, testregelService)

  @Test
  fun `getTestregelAggregates returns correct aggregates`() {
    val testregelBase =
        Testregel(
            id = 1,
            namn = "Test",
            kravId = 5,
            modus = TestregelModus.manuell,
            testregelSchema = "schema",
            testregelId = "1.1.1",
            versjon = 1,
            status = TestregelStatus.publisert,
            datoSistEndra = Instant.now(),
            type = TestregelInnholdstype.nett,
            spraak = TestlabLocale.nb,
            kravTilSamsvar = "svar",
            tema = 2,
            testobjekt = 3,
            innhaldstypeTesting = 4)
    val tema = Tema(id = 2, tema = "Tema")
    val testobjekt = Testobjekt(id = 3, testobjekt = "Objekt")
    val innhaldstype = InnhaldstypeTesting(id = 4, innhaldstype = "Innhald")
    val krav =
        KravWcag2x(
            id = 5,
            tittel = "Krav",
            status = KravStatus.gjeldande,
            innhald = "Innhald",
            gjeldAutomat = true,
            gjeldNettsider = true,
            gjeldApp = false,
            urlRettleiing = "http://example.com",
            prinsipp = WcagPrinsipp.robust,
            retningslinje = WcagRetninglinje.leselig,
            suksesskriterium = "Kriterium",
            samsvarsnivaa = WcagSamsvarsnivaa.AA,
            kommentarBrudd = "Kommentar")

    Mockito.`when`(testregelService.getTestregelList()).thenReturn(listOf(testregelBase))
    Mockito.`when`(testregelService.getTemaForTestregel()).thenReturn(listOf(tema))
    Mockito.`when`(testregelService.getTestobjekt()).thenReturn(listOf(testobjekt))
    Mockito.`when`(testregelService.getInnhaldstypeForTesting()).thenReturn(listOf(innhaldstype))
    Mockito.`when`(kravregisterClient.listKrav()).thenReturn(listOf(krav))

    val result = resource.getTestregelAggregates()

    assertEquals(1, result.size)
    val aggregate = result.first()
    assertEquals(testregelBase.id, aggregate.id)
    assertEquals(testregelBase.namn, aggregate.namn)
    assertEquals(tema, aggregate.tema)
    assertEquals(testobjekt, aggregate.testobjekt)
    assertEquals(innhaldstype, aggregate.innhaldstypeTesting)
    assertEquals(krav, aggregate.krav)
  }
}
