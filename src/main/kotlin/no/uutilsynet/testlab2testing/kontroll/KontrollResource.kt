package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlagFromKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestStatus
import no.uutilsynet.testlab2testing.kontroll.Kontroll.Testreglar
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.InnhaldstypeTesting
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(
    val kontrollDAO: KontrollDAO,
    val maalingService: MaalingService,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testgrunnlagService: TestgrunnlagService,
    val testregelClient: TestregelClient,
) {
  private val logger: Logger = LoggerFactory.getLogger(KontrollResource::class.java)

  data class KontrollListItem(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Sakstype,
      val arkivreferanse: String,
      val kontrolltype: Kontrolltype,
      val virksomheter: List<String>, // liste med orgnummer
      val styringsdataId: Int?
  )

  @GetMapping
  fun getKontroller(): List<KontrollListItem> {
    return kontrollDAO
        .getKontroller()
        .mapCatching { kontrollRows ->
          kontrollRows.map { kontrollDB ->
            val virksomheter = getVirksomheterForKontroll(kontrollDB)

            KontrollListItem(
                kontrollDB.id,
                kontrollDB.tittel,
                kontrollDB.saksbehandler,
                Sakstype.valueOf(kontrollDB.sakstype),
                kontrollDB.arkivreferanse,
                kontrollDB.kontrolltype,
                virksomheter,
                kontrollDB.styringsdataId)
          }
        }
        .getOrElse {
          logger.error("Feilet da jeg skulle hente alle kontroller", it)
          throw RuntimeException(it)
        }
  }

  private fun getVirksomheterForKontroll(kontrollDB: KontrollDAO.KontrollDB): List<String> {
    runCatching { getLoeysingarlistFromUtval(kontrollDB.utval).map { it.orgnummer }.distinct() }
        .fold(
            onSuccess = {
              return it
            },
            onFailure = {
              logger.error("Feil ved henting av virksomheter for kontroll ${kontrollDB.id}", it)
              throw it
            })
  }

  @PostMapping
  fun createKontroll(@RequestBody opprettKontroll: OpprettKontroll): ResponseEntity<Unit> {
    return runCatching {
          val id = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()
          if (opprettKontroll.kontrolltype == Kontrolltype.ForenklaKontroll) {
            maalingService.nyMaaling(id, opprettKontroll).getOrThrow()
          }

          location(id)
        }
        .fold(
            onSuccess = { location -> ResponseEntity.created(location).build() },
            onFailure = {
              logger.error("Feil ved oppretting av kontroll", it)
              ResponseEntity.badRequest().build()
            })
  }

  @GetMapping("/{id}")
  fun getKontroll(@PathVariable id: Int): ResponseEntity<Kontroll> {
    return getKontrollAsResult(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = {
              when (it) {
                is IllegalArgumentException -> ResponseEntity.notFound().build()
                else -> {
                  logger.error("Feil ved henting av kontroll", it)
                  ResponseEntity.internalServerError().build()
                }
              }
            })
  }

  private fun getKontrollAsResult(kontrollId: Int): Result<Kontroll> = runCatching {
    val kontrollDB = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()

    Kontroll(
        kontrollDB.id,
        kontrollDB.kontrolltype,
        kontrollDB.tittel,
        kontrollDB.saksbehandler,
        Sakstype.valueOf(kontrollDB.sakstype),
        kontrollDB.arkivreferanse,
        kontrollDbUtvalToUtval(kontrollDB),
        kontollTestreglarToTestreglar(kontrollDB.testreglar),
        kontrollDB.sideutval)
  }

  private fun kontrollDbUtvalToUtval(kontroll: KontrollDAO.KontrollDB): Utval? {
    return runCatching {
          kontroll.utval?.let { utval ->
            val loeysingar = getLoeysingarlistFromUtval(utval)
            Utval(utval.id, utval.namn, loeysingar, utval.oppretta)
          }
        }
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error("Feil ved henting av løysingar for utval for kontroll ${kontroll.id}")
              throw it
            })
  }

  private fun kontollTestreglarToTestreglar(
      testreglar: KontrollDAO.KontrollDB.Testreglar?
  ): Testreglar? {
    if (testreglar != null) {
      val testregelList = testregelClient.getTestregelListFromIds(testreglar.testregelIdList)
      return Testreglar(testreglar.regelsettId, testregelList)
    }
    return null
  }

  private fun getLoeysingarlistFromUtval(utval: KontrollDAO.KontrollDB.Utval?): List<Loeysing> {
    if (utval == null || utval.loeysingar.isEmpty()) return emptyList()
    val idList = utval.loeysingar.map { it.id }
    return loeysingsRegisterClient.getMany(idList).getOrThrow()
  }

  @DeleteMapping("/{id}")
  fun deleteKontroll(@PathVariable id: Int): ResponseEntity<Unit> {
    return runCatching {
          kontrollDAO.deleteKontroll(id).getOrThrow()
          maalingService.deleteKontrollMaaling(id).getOrThrow()
        }
        .fold(
            onSuccess = { ResponseEntity.noContent().build() },
            onFailure = {
              logger.error("Feil ved sletting av kontroll", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @PutMapping("/{id}")
  fun updateKontroll(
      @PathVariable id: Int,
      @RequestBody updateBody: KontrollUpdate
  ): ResponseEntity<Unit> =
      runCatching {
            require(updateBody.kontroll.id == id) { "id i URL-en og id er ikkje den same" }
            val hasTestresultat = testgrunnlagService.kontrollHasTestresultat(id)

            if (hasTestresultat && updateBody !is KontrollUpdate.Edit) {
              logger.error("test er allereie starta for kontroll: ${id}")
              throw IllegalArgumentException("Test er allereie starta")
            }

            when (updateBody) {
              is KontrollUpdate.Edit -> {
                kontrollDAO.updateKontroll(updateBody.kontroll)
              }
              is KontrollUpdate.Utval -> {
                val (kontroll, utvalId) = updateBody
                kontrollDAO.updateKontroll(kontroll, utvalId).getOrThrow()
              }
              is KontrollUpdate.Testreglar -> {
                val (kontroll, testreglar) = updateBody
                val (regelsettId, testregelIdList) = testreglar
                kontrollDAO.updateKontroll(kontroll, regelsettId, testregelIdList).getOrThrow()
              }
              is KontrollUpdate.Sideutval -> {
                val (kontroll, sideutvalList) = updateBody
                if (sideutvalList.any { it.begrunnelse.isBlank() }) {
                  logger.error("Ugyldig sideutval for kontroll: ${kontroll.id}")
                  throw IllegalArgumentException("Ugyldige sider i sideutval")
                }
                kontrollDAO.updateKontroll(kontroll, sideutvalList).getOrThrow()
              }
            }
            if (updateBody.kontroll.kontrolltype == Kontrolltype.ForenklaKontroll) {
              maalingService.updateMaaling(getKontrollAsResult(id).getOrThrow())
            } else {
              createOrUpdateTestgrunnlag(id)
            }
          }
          .fold(
              onSuccess = { ResponseEntity.noContent().build() },
              onFailure = {
                when (it) {
                  is IllegalArgumentException -> ResponseEntity.badRequest().build()
                  else -> {
                    logger.error("Feil ved oppdatering av kontroll", it)
                    ResponseEntity.internalServerError().build()
                  }
                }
              })

  @GetMapping("sideutvaltype")
  fun getSideutvalType(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(kontrollDAO.getSideutvalType()) }
          .getOrElse {
            logger.error("Feila ved henting av sideutvaltyper", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @GetMapping("/test-status/{kontrollId}")
  fun getTestStatus(
      @PathVariable kontrollId: Int,
  ): ResponseEntity<TestStatus> {
    val hasTestresultat = testgrunnlagService.kontrollHasTestresultat(kontrollId)
    return ResponseEntity.ok(if (hasTestresultat) TestStatus.Started else TestStatus.Pending)
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class OpprettKontroll(
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Sakstype,
      val arkivreferanse: String,
      val kontrolltype: Kontrolltype,
  )

  fun createOrUpdateTestgrunnlag(kontrollId: Int): Result<Int> {
    val kontroll = getKontrollAsResult(kontrollId).getOrThrow()

    val nyttTestgrunnlag =
        NyttTestgrunnlagFromKontroll(
            kontroll.id,
            "Testgrunnlag for kontroll ${kontroll.tittel}",
            OPPRINNELEG_TEST,
            kontroll.sideutvalList,
            kontroll.testreglar?.testregelList ?: emptyList())
    return testgrunnlagService.createOrUpdateFromKontroll(nyttTestgrunnlag)
  }

  @GetMapping("/testmetadata/{kontrollId}")
  fun testingMetadata(@PathVariable kontrollId: Int): KontrollTestingMetadata {
    val kontroll = getKontrollAsResult(kontrollId).getOrThrow()
    val sideutvaltypar = kontrollDAO.getSideutvalType()
    val innholdtypeTestingList = testregelClient.getInnhaldstypeForTesting().getOrThrow()

    val innholdstypeTesting = innhaldstypeTestingForKontroll(kontroll, innholdtypeTestingList)

    val sideutvalType =
        kontroll.sideutvalList.map { sideutval ->
          sideutvaltypar.first { it.id == sideutval.typeId }
        }

    return KontrollTestingMetadata(innholdstypeTesting, sideutvalType)
  }

  private fun innhaldstypeTestingForKontroll(
      kontroll: Kontroll,
      innholdtypeTestingList: List<InnhaldstypeTesting>,
  ): List<InnhaldstypeTesting> {
    val innholdstypeTesting =
        kontroll.testreglar
            ?.testregelList
            ?.mapNotNull { testregel -> testregel.innhaldstypeTesting }
            ?.map { innholdsype -> innholdtypeTestingList.first { it.id == innholdsype } }
    return innholdstypeTesting ?: throw IllegalStateException("Kontroll har ingen testreglar")
  }
}
