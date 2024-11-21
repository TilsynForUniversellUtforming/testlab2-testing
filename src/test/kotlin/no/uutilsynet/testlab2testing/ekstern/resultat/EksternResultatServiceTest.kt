package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
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
    print(resultat)
    assert(resultat.isNotEmpty())
    assert(resultat.size == 2)
    assertThat(resultat).isEqualTo(RapportTestdata.rapportForLoeysing)
    print(resultat)
  }
}

object RapportTestdata {
  val kontrollLoeysingIds = listOf(KontrollIdLoeysingId(1, 1), KontrollIdLoeysingId(2, 2))
  val rapportForLoeysing =
      listOf(
          ResultatOversiktLoeysingEkstern(
              "loysing1", Kontrolltype.ForenklaKontroll, "forenkla kontroll","1.1.1", 1, 0.5, 3, 1, 2),
          ResultatOversiktLoeysingEkstern(
              "loysing1", Kontrolltype.ForenklaKontroll, "forenkla kontroll","1.1.1", 1, 0.4, 3, 1, 2),
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
              3,
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
