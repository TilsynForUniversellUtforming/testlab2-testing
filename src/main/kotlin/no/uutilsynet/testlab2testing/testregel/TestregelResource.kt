package no.uutilsynet.testlab2testing.testregel

import java.net.URI
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.createWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.validateTestregel
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/testreglar")
class TestregelResource(
    val testregelDAO: TestregelDAO,
    val maalingDAO: MaalingDAO,
    val kravregisterClient: KravregisterClient
) {

  val logger = LoggerFactory.getLogger(TestregelResource::class.java)

  private val locationForId: (Int) -> URI = { id -> URI("/v1/testreglar/${id}") }

  @GetMapping("{id}")
  fun getTestregel(@PathVariable id: Int): ResponseEntity<Testregel> =
      testregelDAO.getTestregel(id)?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

  @GetMapping
  fun getTestregelList(
      @RequestParam(required = false) maalingId: Int?,
      @RequestParam(required = false) includeMetadata: Boolean = false
  ): ResponseEntity<List<TestregelBase>> =
      runCatching {
            val testregelList =
                if (maalingId != null) {
                  logger.debug("Henter testreglar for måling $maalingId")
                  testregelDAO.getTestreglarForMaaling(maalingId).getOrThrow()
                } else {
                  testregelDAO.getTestregelList()
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
            kravregisterClient.getWcagKrav(testregelInit.kravId).getOrElse {
              throw RuntimeException("Krav med id ${testregelInit.kravId} finns ikkje")
            }

            testregelDAO.createTestregel(testregelInit)
          },
          locationForId)

  @PutMapping
  fun updateTestregel(@RequestBody testregel: Testregel): ResponseEntity<out Any> =
      executeWithErrorHandling {
        testregel.validateTestregel().getOrThrow()
        testregelDAO.updateTestregel(testregel)
      }

  @DeleteMapping("{testregelId}")
  fun deleteTestregel(@PathVariable("testregelId") testregelId: Int): ResponseEntity<out Any> =
      executeWithErrorHandling {
        val maalingTestregelUsageList = testregelDAO.getMaalingTestregelListById(testregelId)
        if (maalingTestregelUsageList.isNotEmpty()) {
          val testregel =
              testregelDAO.getTestregel(testregelId)
                  ?: throw IllegalArgumentException("Testregel med id $testregelId finnes ikke")
          val maalingList =
              maalingDAO
                  .getMaalingList()
                  .filter { maalingTestregelUsageList.contains(it.id) }
                  .map { it.navn }
          throw IllegalArgumentException(
              "Testregel $testregel er i bruk i følgjande målingar: ${maalingList.joinToString(", ")}")
        }
        testregelDAO.deleteTestregel(testregelId)
      }

  @GetMapping("innhaldstypeForTesting")
  fun getInnhaldstypeForTesting(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(testregelDAO.getInnhaldstypeForTesting()) }
          .getOrElse {
            logger.error("Feila ved henting av innhaldstype for testing", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @GetMapping("temaForTestreglar")
  fun getTemaForTesreglar(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(testregelDAO.getTemaForTestregel()) }
          .getOrElse {
            logger.error("Feila ved henting av tema for testreglar", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @GetMapping("testobjektForTestreglar")
  fun getTestobjektForTestreglar(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(testregelDAO.getTestobjekt()) }
          .getOrElse {
            logger.error("Feila ved henting av tema for testreglar", it)
            ResponseEntity.internalServerError().body(it.message)
          }
}
