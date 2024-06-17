package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagServiceKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestStatus
import no.uutilsynet.testlab2testing.kontroll.Kontroll.Testreglar
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(
    val kontrollDAO: KontrollDAO,
    val testregelDAO: TestregelDAO,
    val maalingService: MaalingService,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testgrunnlagServiceKontroll: TestgrunnlagServiceKontroll
) {
  private val logger: Logger = LoggerFactory.getLogger(KontrollResource::class.java)

  data class KontrollListItem(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Kontroll.Sakstype,
      val arkivreferanse: String,
      val kontrolltype: Kontroll.Kontrolltype,
      val virksomheter: List<String> // liste med orgnummer
  )

  @GetMapping
  fun getKontroller(): List<KontrollListItem> {
    return kontrollDAO
        .getKontroller()
        .mapCatching { kontrollRows ->
          kontrollRows.map { kontrollDB ->
            val virksomheter =
                kontrollDB.utval
                    ?.loeysingar
                    ?.map { it.id }
                    ?.let { loeysingIdList ->
                      loeysingsRegisterClient.getMany(loeysingIdList).getOrThrow()
                    }
                    ?.map { it.orgnummer }
                    ?.distinct()
                    ?: emptyList()

            KontrollListItem(
                kontrollDB.id,
                kontrollDB.tittel,
                kontrollDB.saksbehandler,
                Kontroll.Sakstype.valueOf(kontrollDB.sakstype),
                kontrollDB.arkivreferanse,
                Kontroll.Kontrolltype.valueOf(kontrollDB.kontrolltype),
                virksomheter)
          }
        }
        .getOrElse {
          logger.error("Feilet da jeg skulle hente alle kontroller", it)
          throw RuntimeException(it)
        }
  }

  @PostMapping
  fun createKontroll(@RequestBody opprettKontroll: OpprettKontroll): ResponseEntity<Unit> {
    return runCatching {
          val id = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()
          if (opprettKontroll.kontrolltype == Kontroll.Kontrolltype.ForenklaKontroll) {
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
    return getKontrollResult(id)
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

  private fun getKontrollResult(kontrollId: Int): Result<Kontroll> = runCatching {
    val kontrollDB = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()

    Kontroll(
        kontrollDB.id,
        Kontroll.Kontrolltype.valueOf(kontrollDB.kontrolltype),
        kontrollDB.tittel,
        kontrollDB.saksbehandler,
        Kontroll.Sakstype.valueOf(kontrollDB.sakstype),
        kontrollDB.arkivreferanse,
        kontrollDB.utval?.let { utval ->
          val idList = utval.loeysingar.map { it.id }
          val loeysingar = loeysingsRegisterClient.getMany(idList).getOrThrow()

          Utval(utval.id, utval.namn, loeysingar, utval.oppretta)
        },
        kontrollDB.testreglar?.let { testreglar ->
          val testregelList = testregelDAO.getMany(testreglar.testregelIdList)
          Testreglar(testreglar.regelsettId, testregelList)
        },
        kontrollDB.sideutval)
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
            val hasTestresultat = testgrunnlagServiceKontroll.kontrollHasTestresultat(id)

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
            if (updateBody.kontroll.kontrolltype == Kontroll.Kontrolltype.ForenklaKontroll) {
              maalingService.updateMaaling(getKontrollResult(id).getOrThrow())
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
    val hasTestresultat = testgrunnlagServiceKontroll.kontrollHasTestresultat(kontrollId)
    return ResponseEntity.ok(if (hasTestresultat) TestStatus.Started else TestStatus.Pending)
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class OpprettKontroll(
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Kontroll.Sakstype,
      val arkivreferanse: String,
      val kontrolltype: Kontroll.Kontrolltype,
  )

  fun createOrUpdateTestgrunnlag(kontrollId: Int): Result<Int> {
    val kontroll = getKontrollResult(kontrollId).getOrThrow()
    val testregelIdList = kontroll.testreglar?.testregelList?.map { it.id } ?: emptyList()
    val sideutval =
        kontroll.sideutvalList
            .map { it.loeysingId }
            .let { loeysingIdList ->
              if (loeysingIdList.isNotEmpty()) {
                kontrollDAO.findSideutvalByKontrollAndLoeysing(kontroll.id, loeysingIdList)
              } else {
                emptyList()
              }
            }

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontroll.id,
            "Testgrunnlag for kontroll ${kontroll.tittel}",
            OPPRINNELEG_TEST,
            sideutval,
            testregelIdList)
    return testgrunnlagServiceKontroll.createOrUpdate(nyttTestgrunnlag)
  }
}
