package no.uutilsynet.testlab2testing.common

import no.uutilsynet.testlab2.constants.*
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.resultat.OpprettTestgrunnlag
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelInit
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.springframework.stereotype.Service
import java.net.URI
import java.time.Instant

@Service
class TestUtils(
    val kontrollDAO: KontrollDAO,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val maalingDAO: MaalingDAO,
) {

  var testregelId: Int = 0

  fun createTestregelKrav(): TestregelKrav {
    return testregelKravObject()
  }

    fun createTestregel(): Testregel {
        return Testregel(
            1,
            testregelId = "1.1.1",
            namn = "Testregel Navn",
            kravId = 1,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            testregelSchema = "1.1.1",
            innhaldstypeTesting = 1,
            tema = 2,
            testobjekt = 3,
        )
    }

  fun createTestgrunnlag(
      opprettTestgrunnlag: OpprettTestgrunnlag,
      kontroll: KontrollDAO.KontrollDB,
  ): Int {

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontrollId = kontroll.id,
            namn = opprettTestgrunnlag.testgrunnlagNamn,
            type = opprettTestgrunnlag.testgrunnlagType,
            sideutval = kontroll.sideutval,
            testregelIdList = kontroll.testreglar!!.testregelIdList)
    val testgrunnlagId = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag).getOrThrow()

    return testgrunnlagId
  }

  fun createKontroll(
      kontrollNamn: String,
      kontrolltype: Kontrolltype,
      loeysingId: List<Int>,
      testregelId: Int
  ): KontrollDAO.KontrollDB {

    val (kontrollDAO, kontrollId, kontroll) = opprettKontroll(kontrollNamn, kontrolltype)

    opprettUtvalg(kontrollDAO, kontroll, loeysingId)
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregelId))

    return kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
  }

  private fun opprettUtvalg(
      kontrollDAO: KontrollDAO,
      kontroll: Kontroll,
      loeysingId: List<Int> = listOf(1)
  ) {

    val sideUtval =
        loeysingId.map {
          SideutvalBase(it, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null)
        }

    /* Add sideutval */
    kontrollDAO.updateKontroll(kontroll, sideUtval)
  }

  private fun opprettKontroll(
      kontrollNamn: String,
      kontrolltype: Kontrolltype
  ): Triple<KontrollDAO, Int, Kontroll> {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            kontrollNamn, "Ola Nordmann", Sakstype.Arkivsak, "1234", kontrolltype)

    val kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            kontrolltype,
            opprettKontroll.tittel,
            opprettKontroll.saksbehandler,
            opprettKontroll.sakstype,
            opprettKontroll.arkivreferanse,
        )
    return Triple(kontrollDAO, kontrollId, kontroll)
  }

  fun createTestMaaling(
      testregelIds: List<Int>,
      loeysingList: List<Int>,
      maalingNamn: String,
      kontrollId: Int
  ): Int {
    val maalingId =
        maalingDAO.createMaaling(
            maalingNamn, Instant.now(), loeysingList, testregelIds, CrawlParameters())
    maalingDAO.updateKontrollId(maalingId, kontrollId)

    return maalingId
  }

  fun testregelKravObject(): TestregelKrav {

    return TestregelKrav(id = 1, namn = "Test", krav = kravWcag2xObject(), testregelId = "1.1.1")
  }

  fun kravWcag2xObject(): no.uutilsynet.testlab2testing.testregel.krav.KravWcag2x {
    val krav =
        no.uutilsynet.testlab2testing.testregel.krav.KravWcag2x(
            id = 1,
            tittel = "Nett-1.1.1a Ikke-lenkede bilder har tekstalternativ ",
            status = KravStatus.gjeldande,
            innhald = "Nett-1.1.1a Ikke-lenkede bilder har tekstalternativ",
            gjeldAutomat = false,
            gjeldApp = true,
            gjeldNettsider = true,
            urlRettleiing = URI("https://www.example.com").toURL(),
            prinsipp = WcagPrinsipp.mulig_aa_oppfatte,
            suksesskriterium = "1.1.1",
            samsvarsnivaa = WcagSamsvarsnivaa.A,
            retningslinje = WcagRetninglinje.tekst_alternativ,
            kommentarBrudd = "Har ikkje tekstalternativ",
        )
    return krav
  }

  fun testregelInitObject(): TestregelInit {
    return TestregelInit(
        testregelId = "1.1.1",
        namn = "Testregel Navn",
        kravId = 1,
        status = TestregelStatus.publisert,
        type = TestregelInnholdstype.nett,
        modus = TestregelModus.automatisk,
        spraak = TestlabLocale.nb,
        testregelSchema = "1.1.1",
        innhaldstypeTesting = 1,
        tema = 2,
        testobjekt = 3,
        kravTilSamsvar = "svar",
    )
  }
}
