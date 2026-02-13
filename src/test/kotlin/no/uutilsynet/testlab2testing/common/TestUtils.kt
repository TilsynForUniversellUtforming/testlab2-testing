package no.uutilsynet.testlab2testing.common

import java.net.URI
import java.time.Instant
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
import no.uutilsynet.testlab2testing.testregel.model.*
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import org.springframework.stereotype.Service

@Service
class TestUtils(
    val kontrollDAO: KontrollDAO,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val maalingDAO: MaalingDAO,
    val aggregeringDAO: AggregeringDAO,
) {

  var testregelId: Int = 0

  fun createTestregelKrav(): TestregelKrav {
    return testregelKravObject()
  }

  fun createTestregelAggregate(testregelId: Int = 1): TestregelAggregate {
    return TestregelAggregate(
        id = testregelId,
        testregelId = "1.1.1",
        namn = "Testregel Navn",
        modus = TestregelModus.automatisk,
        tema = Tema(id = 2, tema = "Bilder"),
        krav = kravWcag2xObject(),
    )
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
      testregelIds: List<Int>
  ): KontrollDAO.KontrollDB {

    val (kontrollId, kontroll) = opprettKontroll(kontrollNamn, kontrolltype)

    opprettUtvalg(kontroll, loeysingId)
    kontrollDAO.updateKontroll(kontroll, null, testregelIds)

    return kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
  }

  private fun opprettUtvalg(kontroll: Kontroll, loeysingId: List<Int> = listOf(1)) {

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
  ): Pair<Int, Kontroll> {
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
    return Pair(kontrollId, kontroll)
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

  fun createTestMaalingar(
      maalingNamn: List<String>,
      idTestregels: List<Int> = listOf(testregelId)
  ): List<Int> {
    return maalingNamn.map {
      val kontroll =
          createKontroll(
              "Forenkla kontroll 20204", Kontrolltype.ForenklaKontroll, listOf(1), idTestregels)

      createTestMaaling(idTestregels, kontroll, it)
    }
  }

  fun createTestMaaling(
      testregelIds: List<Int>,
      kontroll: KontrollDAO.KontrollDB,
      maalingNamn: String
  ): Int {
    val loeysingList = kontroll.sideutval.map { it.loeysingId }
    val maalingId =
        createTestMaaling(
            testregelIds, kontroll.sideutval.map { it.loeysingId }, maalingNamn, kontroll.id)

    testregelIds.forEach { createAggregertTestresultat(maalingId, it, null, loeysingList) }

    return maalingId
  }

  fun createAggregertTestresultat(
      maalingId: Int?,
      testregelId: Int,
      testgrunnlagId: Int?,
      loeysungIds: List<Int> = listOf(1),
  ): List<Int> {

    return loeysungIds.map {
      val aggregeringTestregel =
          AggregeringPerTestregelDB(
              maalingId,
              it,
              testregelId,
              1,
              listOf(1, 2),
              6,
              3,
              1,
              1,
              1,
              1,
              0,
              0.5,
              0.5,
              testgrunnlagId)

      aggregeringDAO.createAggregertResultatTestregel(aggregeringTestregel)
    }
  }
}
