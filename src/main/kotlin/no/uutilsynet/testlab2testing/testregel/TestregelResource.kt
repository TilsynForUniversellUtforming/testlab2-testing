package no.uutilsynet.testlab2testing.testregel

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.createWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.testregel.TestregelDTO.Companion.validateTestregel
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
class TestregelResource(val testregelDAO: TestregelDAO, val maalingDAO: MaalingDAO) {

  val logger = LoggerFactory.getLogger(TestregelResource::class.java)

  private val locationForId: (Int) -> URI = { id -> URI("/v1/testreglar/${id}") }

  @GetMapping("{id}")
  fun getTestregel(@PathVariable id: Int): ResponseEntity<TestregelDTO> =
      testregelDAO.getTestregelResponse(id)?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

  @GetMapping
  fun getTestregelList(@RequestParam(required = false) maalingId: Int?): ResponseEntity<out Any> =
      runCatching {
            if (maalingId != null) {
              logger.debug("Henter testreglar for måling $maalingId")
              ResponseEntity.ok(testregelDAO.getTestregelResponseForMaaling(maalingId))
            } else {
              ResponseEntity.ok(testregelDAO.getTestregelListResponse())
            }
          }
          .getOrElse {
            val errorMessage =
                if (maalingId != null) "Feila ved henting av testreglar for måling $maalingId"
                else "Feila ved henting av testreglar"
            logger.error(errorMessage, it)
            ResponseEntity.internalServerError().body(it.message)
          }

  @PostMapping
  fun createTestregel(@RequestBody testregelInit: TestregelInit): ResponseEntity<out Any> =
      createWithErrorHandling(
          {
            validateNamn(testregelInit.name).getOrThrow()
            validateKrav(testregelInit.krav).getOrThrow()
            validateSchema(testregelInit.testregelSchema, testregelInit.type).getOrThrow()

            testregelDAO.createTestregel(testregelInit)
          },
          locationForId)

  @PutMapping
  fun updateTestregel(@RequestBody testregelrequest: TestregelDTO): ResponseEntity<out Any> =
      executeWithErrorHandling {
        testregelrequest.validateTestregel().getOrThrow()
        val testregel = testregelResponseToTestregel(testregelrequest)
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

  fun testregelResponseToTestregel(testregelDTO: TestregelDTO): Testregel {
    return Testregel(
        testregelDTO.id,
        testregelDTO.name,
        1,
        testregelDTO.name,
        testregelDTO.krav,
        TestregelStatus.publisert,
        Instant.now(),
        TestregelInnholdstype.nett,
        testregelDTO.type,
        TestlabLocale.nb,
        1,
        1,
        testregelDTO.krav,
        testregelDTO.testregelSchema,
        testregelDTO.innhaldstypeTesting)
  }

  fun testregelListToTestregelResponseList(
      testregelResultList: Result<List<Testregel>>
  ): Result<List<TestregelDTO>> {
    return testregelResultList.map { it.map { TestregelDTO(it) } }
  }
}
