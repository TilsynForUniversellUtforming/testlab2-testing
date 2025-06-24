package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.createWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.validateTestregel
import no.uutilsynet.testlab2testing.testregel.import.TestregelImportService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("v1/testreglar")
class TestregelResource(
    val kravregisterClient: KravregisterClient,
    val testregelImportService: TestregelImportService,
    val maalingService: MaalingService
) {

  private final val testregelService: TestregelService = TODO("initialize me")
  val logger = LoggerFactory.getLogger(TestregelResource::class.java)

  private val locationForId: (Int) -> URI = { id -> URI("/v1/testreglar/${id}") }

  @GetMapping("{id}")
  fun getTestregel(@PathVariable id: Int): ResponseEntity<Testregel> {
    return runCatching { testregelService.getTestregel(id) }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @GetMapping
  fun getTestregelList(
      @RequestParam(required = false) maalingId: Int?,
      @RequestParam(required = false) includeMetadata: Boolean = false
  ): ResponseEntity<List<TestregelBase>> =
      runCatching {
            val testregelList =
                if (maalingId != null) {
                  logger.debug("Henter testreglar for måling $maalingId")
                  maalingService.getTestreglarForMaaling(maalingId).getOrThrow()
                } else {
                  testregelService.getTestregelList()
                }
            ResponseEntity.ok(
                testregelList.let {
                  if (includeMetadata) it else it.map { tr -> tr.toTestregelBase() }
                })
          }
          .getOrElse {
            val errorMessage =
                if (maalingId != null) "Feila ved henting av testreglar for måling $maalingId"
                else "Feila ved henting av testreglar"
            logger.error(errorMessage, it)
            ResponseEntity.internalServerError().build()
          }

  @PostMapping
  fun createTestregel(@RequestBody testregelInit: TestregelInit): ResponseEntity<out Any> =
      createWithErrorHandling(
          {
            validateNamn(testregelInit.namn).getOrThrow()
            validateSchema(testregelInit.testregelSchema, testregelInit.modus).getOrThrow()
            validateKrav(testregelInit.kravId)

            testregelService.createTestregel(testregelInit)
          },
          locationForId)

  @PutMapping
  fun updateTestregel(@RequestBody testregel: Testregel): ResponseEntity<out Any> =
      executeWithErrorHandling {
        validateKrav(testregel.kravId)
        testregel.validateTestregel().getOrThrow()
        testregelService.updateTestregel(testregel)
      }

  @DeleteMapping("{testregelId}")
  fun deleteTestregel(@PathVariable("testregelId") testregelId: Int): ResponseEntity<out Any> =
      executeWithErrorHandling {
        val maalingTestregelUsageList = maalingService.getMaalingForTestregel(testregelId)
        if (maalingTestregelUsageList.isNotEmpty()) {
          val testregel = testregelService.getTestregel(testregelId)

          val maalingList = maalingService.getMaalingList(maalingTestregelUsageList).map { it.navn }

          throw IllegalArgumentException(
              "Testregel $testregel er i bruk i følgjande målingar: ${maalingList.joinToString(", ")}")
        }
        testregelService.deleteTestregel(testregelId)
      }

  @GetMapping("innhaldstypeForTesting")
  fun getInnhaldstypeForTesting(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(testregelService.getInnhaldstypeForTesting()) }
          .getOrElse {
            logger.error("Feila ved henting av innhaldstype for testing", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @GetMapping("temaForTestreglar")
  fun getTemaForTesreglar(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(testregelService.getTemaForTestregel()) }
          .getOrElse {
            logger.error("Feila ved henting av tema for testreglar", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @GetMapping("testobjektForTestreglar")
  fun getTestobjektForTestreglar(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(testregelService.getTestobjekt()) }
          .getOrElse {
            logger.error("Feila ved henting av tema for testreglar", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @PostMapping("import")
  fun importTestreglar() {
    runCatching {
      val testreglarNett = testregelImportService.importTestreglarNett().getOrThrow()
      val testreglarApp = testregelImportService.importTestreglarApp().getOrThrow()
      logger.info("Importerte testreglar for nett: $testreglarNett  for app: $testreglarApp")
    }
  }

  fun validateKrav(kravId: Int) =
      runCatching { kravregisterClient.getWcagKrav(kravId) }
          .getOrElse { throw IllegalArgumentException("Krav med id $kravId finns ikkje") }
}
