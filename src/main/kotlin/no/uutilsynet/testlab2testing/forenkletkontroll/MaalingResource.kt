package no.uutilsynet.testlab2testing.forenkletkontroll

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil
import no.uutilsynet.testlab2testing.common.validateStatus
import no.uutilsynet.testlab2testing.forenkletkontroll.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.firstMessage
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.AutotesterTestresultat
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/maalinger")
class MaalingResource(
    val maalingDAO: MaalingDAO,
    val sideutvalDAO: SideutvalDAO,
    val maalingService: MaalingService,
    val brukarService: BrukarService,
    val maalingTestingService: MaalingTestingService,
    val maalingCrawlingService: MaalingCrawlingService,
) {

  data class NyMaalingDTO(
      val navn: String,
      val loeysingIdList: List<Int>?,
      val utvalId: UtvalId?,
      val testregelIdList: List<Int>,
      val crawlParameters: CrawlParameters?
  )

  private val logger = LoggerFactory.getLogger(MaalingResource::class.java)

  @PostMapping
  fun nyMaaling(@RequestBody dto: NyMaalingDTO): ResponseEntity<Any> =
      maalingService
          .nyMaaling(dto)
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception ->
                logger.error(exception.message)
                ErrorHandlingUtil.handleErrors(exception)
              })

  @PutMapping
  fun updateMaaling(@RequestBody dto: EditMaalingDTO): ResponseEntity<out Any> =
      maalingService
          .updateMaaling(dto)
          .fold(
              { ResponseEntity.ok().build() },
              { exception ->
                logger.error("Feila da vi skulle oppdatere målinga ${dto.id}", exception)
                ErrorHandlingUtil.handleErrors(exception)
              })

  @DeleteMapping("{id}")
  fun deleteMaaling(@PathVariable id: Int): ResponseEntity<out Any> =
      maalingService.deleteMaaling(id).fold({ ResponseEntity.ok().build() }) { exception ->
        ErrorHandlingUtil.handleErrors(exception)
      }

  @GetMapping
  fun list(): List<MaalingListElement> {
    return maalingDAO.getMaalingList()
  }

  @GetMapping("{id}")
  fun getMaaling(@PathVariable id: Int): ResponseEntity<out Any> {
    return runCatching { maalingDAO.getMaaling(id) }
        .fold(
            onSuccess = { it.let { ResponseEntity.ok(it) } },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("{maalingId}/testresultat")
  fun getTestResultatList(
      @PathVariable maalingId: Int,
      @RequestParam loeysingId: Int?
  ): ResponseEntity<Any> =
      getTestresultat(maalingId, loeysingId)
          .fold(
              { testResultatList -> ResponseEntity.ok(testResultatList) },
              { error ->
                logger.error(
                    "Feila da vi skulle hente fullt resultat for målinga $maalingId", error)
                ResponseEntity.internalServerError().body(error.firstMessage())
              })

  fun getTestresultat(maalingId: Int, loeysingId: Int?): Result<List<AutotesterTestresultat>> {

    return runCatching { maalingService.getTestresultatMaalingLoeysing(maalingId, loeysingId) }
        .fold(onSuccess = { it }, onFailure = { Result.failure(it) })
  }

  @Operation(
      summary = "Hentar aggregert resultat for ei måling",
      description =
          "Aggregerar resultat frå alle testkøyringar for ei måling. Resultatane kan aggregerast på testresultat, suksesskriterium eller side. Dette velger du med parameteret `aggregeringstype`.",
      parameters =
          [
              Parameter(
                  name = "aggregeringstype",
                  `in` = ParameterIn.QUERY,
                  description =
                      "Kva slags aggregering vil du ha? testresultat, suksesskriterium eller side.",
                  schema =
                      Schema(
                          type = "string",
                          defaultValue = "testresultat",
                          allowableValues = ["testresultat", "suksesskriterium", "side"])),
          ],
      responses =
          [
              ApiResponse(responseCode = "200", description = "Returnerer aggregeringa"),
              ApiResponse(responseCode = "400", description = "Målinga er ikkje ferdig testa"),
              ApiResponse(responseCode = "404", description = "Målinga vart ikkje funne"),
              ApiResponse(responseCode = "500", description = "Andre feil")])
  @GetMapping("{maalingId}/testresultat/aggregering")
  fun getAggregering(
      @PathVariable maalingId: Int,
      @RequestParam aggregeringstype: String = "testregel"
  ): ResponseEntity<Any> {

    return when (aggregeringstype) {
      "testresultat" -> maalingService.hentEllerGenererAggregeringPrTestregel(maalingId)
      "suksesskriterium" -> maalingService.hentEllerGenererAggregeringPrSuksesskriterium(maalingId)
      "side" -> maalingService.hentEllerGenererAggregeringPrSide(maalingId)
      else -> throw IllegalArgumentException("Ugyldig aggregeringstype: $aggregeringstype")
    }
  }

  @GetMapping("{maalingId}/crawlresultat/nettsider")
  fun getCrawlResultatNettsider(
      @PathVariable maalingId: Int,
      @RequestParam loeysingId: Int
  ): ResponseEntity<List<URL>> =
      sideutvalDAO.getCrawlResultatNettsider(maalingId, loeysingId).let { ResponseEntity.ok(it) }

  @PutMapping("{id}/status")
  fun putNewStatus(@PathVariable id: Int, @RequestBody statusDTO: StatusDTO): ResponseEntity<Any> {
    return runCatching<ResponseEntity<Any>> {
          val maaling = maalingDAO.getMaaling(id)
          val newStatus = validateStatus(statusDTO.status).getOrThrow()
          ResponseEntity.badRequest().build<Any>()

          val brukar = brukarService.getCurrentUser()

          when (maaling) {
            is Maaling.Planlegging -> {
              putNewStatusMaalingPlanlegging(maaling, newStatus)
            }
            is Maaling.Kvalitetssikring -> {
              putNewStatusMaalingKvalitetssikring(newStatus, statusDTO, maaling, brukar)
            }
            is Maaling.TestingFerdig -> {
              putNewStatusMaalingTestingFerdig(newStatus, statusDTO, maaling, brukar)
            }
            else ->
                throw IllegalArgumentException("Ugyldig målingstype: ${maaling::class.simpleName}")
          }
        }
        .getOrElse { exception ->
          logger.error(exception.message)
          when (exception) {
            is NullPointerException -> ResponseEntity.notFound().build()
            is IllegalArgumentException ->
                ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(exception.message)
            else -> ResponseEntity.internalServerError().body(exception.message)
          }
        }
  }

  @GetMapping("kontroll/{kontrollId}")
  fun getMaalingIdFromKontrollId(@PathVariable kontrollId: Int): ResponseEntity<Int> {
    return maalingDAO.getMaalingIdFromKontrollId(kontrollId)?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.badRequest().build()
  }

  @GetMapping("aggregering/reimport")
  fun reimportAggregering(@RequestParam maalingId: Int, @RequestParam loeysingId: Int?) {
    maalingService.reimportAggregeringar(maalingId, loeysingId)
  }

  private fun putNewStatusMaalingTestingFerdig(
      newStatus: Status,
      statusDTO: StatusDTO,
      maaling: Maaling.TestingFerdig,
      brukar: Brukar
  ): ResponseEntity<Any> {
    return runBlocking(Dispatchers.IO) {
      when (newStatus) {
        Status.Testing -> maalingTestingService.restartTesting(statusDTO, maaling, brukar)
        else -> throw IllegalArgumentException("Ugyldig status: $newStatus")
      }
    }
  }

  private fun putNewStatusMaalingKvalitetssikring(
      newStatus: Status,
      statusDTO: StatusDTO,
      maaling: Maaling.Kvalitetssikring,
      brukar: Brukar
  ): ResponseEntity<Any> {
    return runBlocking(Dispatchers.IO) {
      when (newStatus) {
        Status.Crawling -> maalingCrawlingService.restartCrawling(statusDTO, maaling)
        Status.Testing -> maalingTestingService.startTesting(maaling, brukar)
      }
    }
  }

  private fun putNewStatusMaalingPlanlegging(
      maaling: Maaling.Planlegging,
      newStatus: Status
  ): ResponseEntity<Any> {
    return runBlocking(Dispatchers.IO) {
      maaling.crawlParameters.validateParameters()
      when (newStatus) {
        Status.Crawling -> maalingCrawlingService.startCrawling(maaling)
        else -> throw IllegalArgumentException("Ugyldig status: $newStatus")
      }
    }
  }

  data class StatusDTO(val status: String, val loeysingIdList: List<Int>?)
}
