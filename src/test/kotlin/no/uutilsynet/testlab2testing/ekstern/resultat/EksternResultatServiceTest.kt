package no.uutilsynet.testlab2testing.ekstern.resultat

import java.time.Instant
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.resultat.LoeysingResultat
import no.uutilsynet.testlab2testing.resultat.Resultat
import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(properties = ["spring.datasource.url: jdbc:tc:postgresql:16-alpine:///test-db"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EksternResultatServiceTest(@Autowired val eksternResultatService: EksternResultatService) {

  @MockBean lateinit var eksternResultatDAO: EksternResultatDAO

  @MockBean lateinit var resultatService: ResultatService

  @Test
  fun getRapportForLoeysing() {
    Mockito.`when`(eksternResultatDAO.findKontrollLoeysingFromRapportId("rapportId"))
        .thenReturn(Result.success(RapportTestdata.kontrollLoeysingIds))
    Mockito.`when`(resultatService.getKontrollLoeysingResultatIkkjeRetest(1, 1))
        .thenReturn(RapportTestdata.restresultat)
    val resultat = eksternResultatService.getRapportForLoeysing("rapportId", 1)
    assert(resultat.isNotEmpty())
    assert(resultat.size == 2)
    assertThat(resultat).isEqualTo(RapportTestdata.rapportForLoeysing)
  }

  @Test
  fun toTestListEkstern() {

    val expectedLoeysingar: List<LoeysingResultat> =
        listOf(
            LoeysingResultat(
                1,
                "Loeysingsnamn",
                "testverksemd",
                0.5,
                TestgrunnlagType.OPPRINNELEG_TEST,
                4,
                2,
                2,
                listOf("testar"),
                1),
            LoeysingResultat(
                2,
                "Loeysingsnamn2",
                "testverksemd",
                0.5,
                TestgrunnlagType.OPPRINNELEG_TEST,
                4,
                2,
                2,
                listOf("testar"),
                2))

    val expectedResultat =
        Resultat(
            1,
            "Forenkla kontroll 20204",
            Kontrolltype.ForenklaKontroll,
            TestgrunnlagType.OPPRINNELEG_TEST,
            LocalDate.now(),
            true,
            expectedLoeysingar)

    Mockito.`when`(resultatService.getKontrollResultatMedType(1, Kontrolltype.ForenklaKontroll))
        .thenReturn(listOf(expectedResultat))

    val testListElementDB =
        TestListElementDB("1", 1, 1, Kontrolltype.ForenklaKontroll, Instant.now())

    val expected =
        TestEkstern("1", 1, "Loeysingsnamn", 0.5, Kontrolltype.ForenklaKontroll, Instant.now())

    val result = eksternResultatService.toTestListEkstern(testListElementDB)

    assertThat(result.size).isNotEqualTo(0)
    assertThat(result[0].loeysingId).isEqualTo(expected.loeysingId)
    assertThat(result[0].loeysingNamn).isEqualTo(expected.loeysingNamn)
    assertThat(result[0].score).isEqualTo(expected.score)
    assertThat(result[0].kontrollType).isEqualTo(expected.kontrollType)
  }
}

object RapportTestdata {
  val kontrollLoeysingIds = listOf(KontrollIdLoeysingId(1, 1), KontrollIdLoeysingId(2, 2))
  val rapportForLoeysing =
      listOf(
          ResultatOversiktLoeysingEkstern(
              "loysing1",
              Kontrolltype.ForenklaKontroll,
              "Forenkla kontroll",
              "1.1.1",
              2,
              0.5,
              3,
              1,
              2),
          ResultatOversiktLoeysingEkstern(
              "loysing1",
              Kontrolltype.ForenklaKontroll,
              "Forenkla kontroll",
              "1.2.1",
              2,
              0.4,
              3,
              1,
              2),
      )
  val restresultat =
      listOf(
          ResultatOversiktLoeysing(
              1,
              "loysing1",
              Kontrolltype.ForenklaKontroll,
              "Forenkla kontroll",
              listOf("Meg"),
              0.5,
              2,
              "1.1.1",
              3,
              1,
              2),
          ResultatOversiktLoeysing(
              1,
              "loysing1",
              Kontrolltype.ForenklaKontroll,
              "Forenkla kontroll",
              listOf("Meg"),
              0.4,
              2,
              "1.2.1",
              3,
              1,
              2),
      )
}
