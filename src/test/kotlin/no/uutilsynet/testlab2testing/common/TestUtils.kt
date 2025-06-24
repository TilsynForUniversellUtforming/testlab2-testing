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
import no.uutilsynet.testlab2testing.testregel.TestConstants
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.springframework.stereotype.Service

@Service
class TestUtils(
    val kontrollDAO: KontrollDAO,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val maalingDAO: MaalingDAO,
    val testregelService: TestregelService
) {

  var testregelId: Int = 0

  fun createTestregel(): Int {

    val temaId = testregelService.createTema("Bilder")

    val innholdstypeTesting = testregelService.createInnhaldstypeForTesting("Tekst")

    val testregelInit =
        TestregelInit(
            testregelId = "QW-ACT-R1",
            namn = TestConstants.name,
            kravId = TestConstants.testregelTestKravId,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            spraak = TestlabLocale.nb,
            testregelSchema = TestConstants.testregelSchemaAutomatisk,
            innhaldstypeTesting = innholdstypeTesting,
            tema = temaId,
            testobjekt = 1,
            kravTilSamsvar = "")

    return testregelService.createTestregel(testregelInit)
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
}
