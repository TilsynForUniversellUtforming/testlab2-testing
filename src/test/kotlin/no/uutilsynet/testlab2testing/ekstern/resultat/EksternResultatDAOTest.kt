package no.uutilsynet.testlab2testing.ekstern.resultat

import java.time.Instant
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(properties = ["spring.datasource.url= jdbc:tc:postgresql:16-alpine:///test-db"])
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EksternResultatDAOTest(
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val maalingDAO: MaalingDAO
) {

  var kontrollId: Int = 0
  val crawlParams = CrawlParameters(10, 10)

  @BeforeEach
  fun setup() {
    val nyKontroll =
        KontrollResource.OpprettKontroll(
            "test", "saksbehandler", Sakstype.Arkivsak, "2025/1", Kontrolltype.ForenklaKontroll)

    kontrollId = kontrollDAO.createKontroll(nyKontroll).getOrThrow()
    val maalingId =
        maalingDAO.createMaaling(
            "Testmaaling", Instant.now(), emptyList(), emptyList(), crawlParams)

    maalingDAO.updateKontrollId(kontrollId, maalingId)
    eksternResultatDAO.publiserMaalingResultat(maalingId, 1)
  }

  @Test
  fun erKontrollPublisertReturnTrue() {

    val resultat = eksternResultatDAO.erKontrollPublisert(kontrollId, Kontrolltype.ForenklaKontroll)

    assertEquals(true, resultat)
  }

  @Test
  fun erKontrollPublisertReturnFalse() {

    val resultat = eksternResultatDAO.erKontrollPublisert(2, Kontrolltype.ForenklaKontroll)

    assertEquals(false, resultat)
  }

  @Test
  fun erKontrollPublisertSjekkarPaaKontrolltype() {

    val resultat = eksternResultatDAO.erKontrollPublisert(1, Kontrolltype.InngaaendeKontroll)

    assertEquals(false, resultat)
  }

  @Test
  fun erKontrollPublisertReturnerarTrueMedFleireMaalingarPrKontroll() {
    val maalingId2 =
        maalingDAO.createMaaling(
            "Testmaaling", Instant.now(), emptyList(), emptyList(), crawlParams)
    maalingDAO.updateKontrollId(kontrollId, maalingId2)
    eksternResultatDAO.publiserMaalingResultat(maalingId2, 1)

    val resultat = eksternResultatDAO.erKontrollPublisert(1, Kontrolltype.ForenklaKontroll)

    assertEquals(true, resultat)
  }
}
