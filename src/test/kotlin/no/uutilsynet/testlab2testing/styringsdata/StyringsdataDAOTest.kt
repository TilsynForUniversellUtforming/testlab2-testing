package no.uutilsynet.testlab2testing.styringsdata

import java.time.Instant
import java.time.LocalDate
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
class StyringsdataDAOTest(
    @Autowired private val styringsdataDAO: StyringsdataDAO,
    @Autowired val kontrollDAO: KontrollDAO,
) {

  private var styringsdataId: Int by Delegates.notNull()
  private var kontrollId: Int by Delegates.notNull()

  @BeforeAll
  fun setUp() {
    kontrollId = createTestKontroll()
    styringsdataId = createTestStyringsdata()
  }

  @Test
  fun testGetStyringsdata() {
    val styringsdataList = styringsdataDAO.getStyringsdata(styringsdataId!!)
    assertThat(styringsdataList).isNotEmpty
    val styringsdata = styringsdataList.first()

    assertThat(styringsdata.id).isEqualTo(styringsdataId)
    assertThat(styringsdata.ansvarleg).isEqualTo("SD")
    assertThat(styringsdata.reaksjon).isEqualTo(Reaksjonstype.reaksjon)
  }

  @Test
  fun testListStyringsdataForKontroll() {
    val styringsdataList = styringsdataDAO.listStyringsdataForKontroll(1)
    assertThat(styringsdataList).isNotEmpty
  }

  @Test
  fun testCreateStyringsdataWithoutPaaleggAndOthers() {
    val styringsdataId = createTestStyringsdata()
    assertThat(styringsdataId).isNotNull
  }

  @Test
  fun testUpdateStyringsdataWithPaalegg() {
    val paalegg =
        Paalegg(
            id = null,
            vedtakDato = LocalDate.now().minusDays(10),
            frist = LocalDate.now().plusDays(20))

    val originalStyringsdata = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata(
              id = styringsdata.id,
              loeysingId = styringsdata.kontrollId,
              kontrollId = styringsdata.loeysingId,
              ansvarleg = styringsdata.ansvarleg,
              oppretta = styringsdata.oppretta,
              frist = styringsdata.frist,
              reaksjon = styringsdata.reaksjon,
              paaleggReaksjon = styringsdata.paaleggReaksjon,
              paaleggKlageReaksjon = styringsdata.paaleggKlageReaksjon,
              botReaksjon = styringsdata.botReaksjon,
              botKlageReaksjon = styringsdata.botKlageReaksjon,
              paalegg = paalegg,
              paaleggKlage = null,
              bot = null,
              botKlage = null,
              sistLagra = styringsdata.sistLagra)
        }

    styringsdataDAO.updateStyringsdata(styringsdataId!!, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedPaalegg = styringsdataDAO.getPaalegg(updatedData.paaleggId!!)
    assertThat(updatedPaalegg).isNotNull
    assertThat(updatedPaalegg!!.vedtakDato).isEqualTo(paalegg.vedtakDato)
  }

  @Test
  fun testUpdateStyringsdataWithPaaleggKlage() {
    val paaleggKlage =
        Klage(
            id = null,
            klageMottattDato = LocalDate.now().minusDays(5),
            klageAvgjortDato = LocalDate.now().plusDays(15),
            resultatKlageTilsyn = ResultatKlage.stadfesta,
            klageDatoDepartement = LocalDate.now().plusDays(20),
            resultatKlageDepartement = ResultatKlage.stadfesta)

    val originalStyringsdata = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata(
              id = styringsdata.id,
              loeysingId = styringsdata.kontrollId,
              kontrollId = styringsdata.loeysingId,
              ansvarleg = styringsdata.ansvarleg,
              oppretta = styringsdata.oppretta,
              frist = styringsdata.frist,
              reaksjon = styringsdata.reaksjon,
              paaleggReaksjon = styringsdata.paaleggReaksjon,
              paaleggKlageReaksjon = styringsdata.paaleggKlageReaksjon,
              botReaksjon = styringsdata.botReaksjon,
              botKlageReaksjon = styringsdata.botKlageReaksjon,
              paalegg = styringsdata.paaleggId?.let { styringsdataDAO.getPaalegg(it) },
              paaleggKlage = paaleggKlage,
              bot = null,
              botKlage = null,
              sistLagra = styringsdata.sistLagra)
        }

    styringsdataDAO.updateStyringsdata(styringsdataId!!, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedPaaleggKlage = styringsdataDAO.getKlage(updatedData.paaleggKlageId!!)
    assertThat(updatedPaaleggKlage).isNotNull
    assertThat(updatedPaaleggKlage!!.klageMottattDato).isEqualTo(paaleggKlage.klageMottattDato)
  }

  @Test
  fun testUpdateStyringsdataWithBot() {
    val bot =
        Bot(
            id = null,
            beloepDag = 200,
            oekingEtterDager = 5,
            oekningType = BotOekningType.kroner,
            oekingSats = 20,
            vedtakDato = LocalDate.now().plusDays(30),
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusDays(60),
            kommentar = "Updated Test Bot")

    val originalStyringsdata = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata(
              id = styringsdata.id,
              loeysingId = styringsdata.kontrollId,
              kontrollId = styringsdata.loeysingId,
              ansvarleg = styringsdata.ansvarleg,
              oppretta = styringsdata.oppretta,
              frist = styringsdata.frist,
              reaksjon = styringsdata.reaksjon,
              paaleggReaksjon = styringsdata.paaleggReaksjon,
              paaleggKlageReaksjon = styringsdata.paaleggKlageReaksjon,
              botReaksjon = styringsdata.botReaksjon,
              botKlageReaksjon = styringsdata.botKlageReaksjon,
              paalegg = styringsdata.paaleggId?.let { styringsdataDAO.getPaalegg(it) },
              paaleggKlage = styringsdata.paaleggKlageId?.let { styringsdataDAO.getKlage(it) },
              bot = bot,
              botKlage = null,
              sistLagra = styringsdata.sistLagra)
        }

    styringsdataDAO.updateStyringsdata(styringsdataId!!, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedBot = styringsdataDAO.getBot(updatedData.botId!!)
    assertThat(updatedBot).isNotNull
    assertThat(updatedBot!!.beloepDag).isEqualTo(bot.beloepDag)
  }

  @Test
  fun testUpdateStyringsdataWithBotKlage() {
    val botKlage =
        Klage(
            id = null,
            klageMottattDato = LocalDate.now().minusDays(5),
            klageAvgjortDato = LocalDate.now().plusDays(15),
            resultatKlageTilsyn = ResultatKlage.stadfesta,
            klageDatoDepartement = LocalDate.now().plusDays(20),
            resultatKlageDepartement = ResultatKlage.stadfesta)

    val originalStyringsdata = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata(
              id = styringsdata.id,
              loeysingId = styringsdata.kontrollId,
              kontrollId = styringsdata.loeysingId,
              ansvarleg = styringsdata.ansvarleg,
              oppretta = styringsdata.oppretta,
              frist = styringsdata.frist,
              reaksjon = styringsdata.reaksjon,
              paaleggReaksjon = styringsdata.paaleggReaksjon,
              paaleggKlageReaksjon = styringsdata.paaleggKlageReaksjon,
              botReaksjon = styringsdata.botReaksjon,
              botKlageReaksjon = styringsdata.botKlageReaksjon,
              paalegg = styringsdata.paaleggId?.let { styringsdataDAO.getPaalegg(it) },
              paaleggKlage = styringsdata.paaleggKlageId?.let { styringsdataDAO.getKlage(it) },
              bot = styringsdata.botId?.let { styringsdataDAO.getBot(it) },
              botKlage = botKlage,
              sistLagra = styringsdata.sistLagra)
        }

    styringsdataDAO.updateStyringsdata(styringsdataId!!, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdata(styringsdataId!!).first()
    val updatedBotKlage = styringsdataDAO.getKlage(updatedData.botKlageId!!)
    assertThat(updatedBotKlage).isNotNull
    assertThat(updatedBotKlage!!.klageMottattDato).isEqualTo(botKlage.klageMottattDato)
  }

  private fun createTestStyringsdata(): Int {
    val styringsdata =
        Styringsdata(
            id = null,
            loeysingId = 1,
            kontrollId = kontrollId,
            ansvarleg = "SD",
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(50),
            reaksjon = Reaksjonstype.reaksjon,
            paaleggReaksjon = Reaksjonstype.reaksjon,
            paaleggKlageReaksjon = Reaksjonstype.reaksjon,
            botReaksjon = Reaksjonstype.reaksjon,
            botKlageReaksjon = Reaksjonstype.reaksjon,
            paalegg = null,
            paaleggKlage = null,
            bot = null,
            botKlage = null,
            sistLagra = Instant.now())
    return styringsdataDAO.createStyringsdata(styringsdata).getOrThrow()
  }

  private fun createTestKontroll(): Int {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Kontroll.Sakstype.Arkivsak,
            "1234",
            Kontroll.Kontrolltype.InngaaendeKontroll)

    return kontrollDAO.createKontroll(opprettKontroll).getOrThrow()
  }
}
