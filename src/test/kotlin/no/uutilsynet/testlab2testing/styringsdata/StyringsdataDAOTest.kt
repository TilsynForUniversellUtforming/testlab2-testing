package no.uutilsynet.testlab2testing.styringsdata

import java.time.Instant
import java.time.LocalDate
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.Bot
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.BotOekningType
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.Klage
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.Paalegg
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.ResultatKlage
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

  private var styringsdataKontrollId: Int by Delegates.notNull()
  private var styringsdataLoeysingId: Int by Delegates.notNull()
  private var kontrollId: Int by Delegates.notNull()

  @BeforeAll
  fun setUp() {
    kontrollId = createTestKontroll()
    styringsdataKontrollId = createTestStyringsdataKontroll()
    styringsdataLoeysingId = createTestStyringsdataLoeysing()
  }

  @Test
  fun testGetStyringsdataKontroll() {
    val styringsdataList = styringsdataDAO.getStyringsdataKontroll(styringsdataKontrollId)
    assertThat(styringsdataList).isNotEmpty
    val styringsdata = styringsdataList.first()

    assertThat(styringsdata.id).isEqualTo(styringsdataKontrollId)
    assertThat(styringsdata.ansvarleg).isEqualTo("SD kontroll")
  }

  @Test
  fun testGetStyringsdataLoeysing() {
    val styringsdataList = styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId)
    assertThat(styringsdataList).isNotEmpty
    val styringsdata = styringsdataList.first()

    assertThat(styringsdata.id).isEqualTo(styringsdataLoeysingId)
    assertThat(styringsdata.ansvarleg).isEqualTo("SD")
    assertThat(styringsdata.reaksjon).isEqualTo(Reaksjonstype.reaksjon)
  }

  @Test
  fun testFindStyringsdataKontroll() {
    val styringdataKontroll = styringsdataDAO.findStyringsdataKontroll(kontrollId)
    assertThat(styringdataKontroll).isNotNull
  }

  @Test
  fun testFindStyringsdataLoeysing() {
    val styringsdataList = styringsdataDAO.findStyringsdataLoeysing(kontrollId)
    assertThat(styringsdataList).isNotEmpty
  }

  @Test
  fun testUpdateStyringsdataKontroll() {
    val original = styringsdataDAO.getStyringsdataKontroll(styringsdataKontrollId).first()

    val update =
        original.copy(
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(1),
            endeligRapportDato = LocalDate.now().plusDays(2),
            foerebelsRapportSendtDato = LocalDate.now().plusDays(3),
            kontrollAvsluttaDato = LocalDate.now().plusDays(4),
            rapportPublisertDato = LocalDate.now().plusDays(5),
            svarFoerebelsRapportDato = LocalDate.now().plusDays(6),
            varselSendtDato = LocalDate.now().plusDays(7),
            status = Styringsdata.Kontroll.StyringsdataKontrollStatus.paagar,
        )

    styringsdataDAO.updateStyringsdataKontroll(original.id!!, update)

    val styringsdataUpdated =
        styringsdataDAO.getStyringsdataKontroll(styringsdataKontrollId).first()

    assertThat(styringsdataUpdated.status)
        .isEqualTo(Styringsdata.Kontroll.StyringsdataKontrollStatus.paagar)
    assertThat(styringsdataUpdated.frist).isEqualTo(LocalDate.now().plusDays(1))
    assertThat(styringsdataUpdated.endeligRapportDato).isEqualTo(LocalDate.now().plusDays(2))
    assertThat(styringsdataUpdated.foerebelsRapportSendtDato).isEqualTo(LocalDate.now().plusDays(3))
    assertThat(styringsdataUpdated.kontrollAvsluttaDato).isEqualTo(LocalDate.now().plusDays(4))
    assertThat(styringsdataUpdated.rapportPublisertDato).isEqualTo(LocalDate.now().plusDays(5))
    assertThat(styringsdataUpdated.svarFoerebelsRapportDato).isEqualTo(LocalDate.now().plusDays(6))
    assertThat(styringsdataUpdated.varselSendtDato).isEqualTo(LocalDate.now().plusDays(7))
  }

  @Test
  fun testUpdateStyringsdataWithPaalegg() {
    val paalegg =
        Paalegg(
            id = null,
            vedtakDato = LocalDate.now().minusDays(10),
            frist = LocalDate.now().plusDays(20))

    val originalStyringsdata =
        styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata.Loeysing(
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

    styringsdataDAO.updateStyringsdataLoeysing(styringsdataLoeysingId, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
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

    val originalStyringsdata =
        styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata.Loeysing(
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

    styringsdataDAO.updateStyringsdataLoeysing(styringsdataLoeysingId, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
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

    val originalStyringsdata =
        styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata.Loeysing(
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

    styringsdataDAO.updateStyringsdataLoeysing(styringsdataLoeysingId, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
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

    val originalStyringsdata =
        styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
    val updatedStyringsdata =
        originalStyringsdata.let { styringsdata ->
          Styringsdata.Loeysing(
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

    styringsdataDAO.updateStyringsdataLoeysing(styringsdataLoeysingId, updatedStyringsdata)

    val updatedData = styringsdataDAO.getStyringsdataLoeysing(styringsdataLoeysingId).first()
    val updatedBotKlage = styringsdataDAO.getKlage(updatedData.botKlageId!!)
    assertThat(updatedBotKlage).isNotNull
    assertThat(updatedBotKlage!!.klageMottattDato).isEqualTo(botKlage.klageMottattDato)
  }

  private fun createTestStyringsdataKontroll(): Int {
    val styringsdata =
        Styringsdata.Kontroll(
            id = null,
            kontrollId = kontrollId,
            ansvarleg = "SD kontroll",
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(50),
            endeligRapportDato = null,
            foerebelsRapportSendtDato = null,
            kontrollAvsluttaDato = null,
            rapportPublisertDato = null,
            status = null,
            svarFoerebelsRapportDato = null,
            varselSendtDato = null,
            sistLagra = Instant.now())
    return styringsdataDAO.createStyringsdataKontroll(styringsdata).getOrThrow()
  }

  private fun createTestStyringsdataLoeysing(): Int {
    val styringsdata =
        Styringsdata.Loeysing(
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
    return styringsdataDAO.createStyringsdataLoeysing(styringsdata).getOrThrow()
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
