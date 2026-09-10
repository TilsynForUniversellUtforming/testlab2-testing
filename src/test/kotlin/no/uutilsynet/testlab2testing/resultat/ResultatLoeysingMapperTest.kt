package no.uutilsynet.testlab2testing.resultat

import java.net.URI
import java.time.LocalDate
import kotlin.jvm.java
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.KravStatus
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.WcagPrinsipp
import no.uutilsynet.testlab2.constants.WcagRetninglinje
import no.uutilsynet.testlab2.constants.WcagSamsvarsnivaa
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.Verksemd
import no.uutilsynet.testlab2testing.resultat.resultMappers.ResultatCalculator
import no.uutilsynet.testlab2testing.resultat.resultMappers.ResultatLoeysingMapper
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravWcag2x
import no.uutilsynet.testlab2testing.testregel.model.Tema
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ResultatLoeysingMapperTest {

  private val testregelCache = mock(TestregelCache::class.java)
  private val resultatMapper = ResultatLoeysingMapper(testregelCache, ResultatCalculator())

  @Test
  fun `toResultatTema maps tema and aggregates score and elements`() {
    val testregelId = 11
    `when`(testregelCache.getTestregelById(testregelId))
        .thenReturn(testregel(testregelId, tema = "Bilder"))

    val result =
        resultatMapper.toResultatTema(
            mapEntry(
                testregelId,
                listOf(
                    resultatDto(
                        testregelId = testregelId,
                        score = 0.5,
                        talElementBrot = 2,
                        talElementSamsvar = 4),
                    resultatDto(
                        testregelId = testregelId,
                        score = 1.0,
                        talElementBrot = 0,
                        talElementSamsvar = 0),
                ),
            ),
        )

    assertEquals("Bilder", result.temaNamn)
    assertEquals(0.5, result.score)
    assertEquals(6, result.talTestaElement)
    assertEquals(2, result.talElementBrot)
    assertEquals(4, result.talElementSamsvar)
  }

  @Test
  fun `toResultatOversiktLoeysing nulls score for ikkje forekomst`() {
    val testregelId = 12
    `when`(testregelCache.getTestregelById(testregelId)).thenReturn(testregel(testregelId))

    val loeysingar =
        LoysingList(
            mapOf(
                1 to
                    Loeysing.Expanded(
                        1,
                        "Løysing 1",
                        URI("https://example.com").toURL(),
                        Verksemd(99, "Verksemd", "123456789"),
                    ),
            ),
        )

    val result =
        resultatMapper.toResultatOversiktLoeysing(
            listOf(
                resultatDto(
                    testregelId = testregelId,
                    score = 0.0,
                    talElementBrot = 0,
                    talElementSamsvar = 0)),
            loeysingar,
        )

    assertEquals(1, result.size)
    assertEquals("Løysing 1", result.first().loeysingNamn)
    assertNull(result.first().score)
  }

  private fun resultatDto(
      testregelId: Int,
      score: Double,
      talElementBrot: Int,
      talElementSamsvar: Int,
  ) =
      ResultatLoeysingDTO(
          id = 1,
          testgrunnlagId = 2,
          namn = "Kontroll",
          typeKontroll = Kontrolltype.ForenklaKontroll,
          testType = TestgrunnlagType.OPPRINNELEG_TEST,
          dato = LocalDate.of(2026, 1, 15),
          testar = listOf("Ola"),
          loeysingId = 1,
          score = score,
          talElementSamsvar = talElementSamsvar,
          talElementBrot = talElementBrot,
          testregelId = testregelId,
      )

  private fun testregel(testregelId: Int, tema: String = "Tema") =
      TestregelAggregate(
          id = testregelId,
          testregelId = "1.1.1",
          namn = "Testregel $testregelId",
          krav =
              KravWcag2x(
                  id = 1,
                  tittel = "Kravtittel",
                  status = KravStatus.gjeldande,
                  innhald = "Innhald",
                  gjeldAutomat = true,
                  gjeldNettsider = true,
                  gjeldApp = true,
                  urlRettleiing = URI("https://example.com/krav").toURL(),
                  prinsipp = WcagPrinsipp.mulig_aa_oppfatte,
                  retningslinje = WcagRetninglinje.tekst_alternativ,
                  suksesskriterium = "1.1.1",
                  samsvarsnivaa = WcagSamsvarsnivaa.A,
                  kommentarBrudd = "Kommentar",
              ),
          modus = TestregelModus.automatisk,
          tema = Tema(2, tema),
      )

  private fun mapEntry(
      key: Int,
      value: List<ResultatLoeysingDTO>,
  ): Map.Entry<Int, List<ResultatLoeysingDTO>> = linkedMapOf(key to value).entries.first()
}
