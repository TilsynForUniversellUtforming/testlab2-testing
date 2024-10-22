package no.uutilsynet.testlab2testing.forenkletkontroll

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateStatus
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.firstMessage
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.sideutval.SideutvalElementAutomatisk
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.validateTestregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
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
import java.net.URI

@RestController
@RequestMapping("v1/maalinger")
class MaalingResource(
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testregelDAO: TestregelDAO,
    val crawlerClient: CrawlerClient,
    val autoTesterClient: AutoTesterClient,
    val aggregeringService: AggregeringService,
    val sideutvalDAO: SideutvalDAO,
    val maalingService: MaalingService,
    val brukarService: BrukarService
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
              { exception -> ErrorHandlingUtil.handleErrors(exception) })

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
  fun getMaaling(@PathVariable id: Int): ResponseEntity<Maaling> =
      maalingDAO.getMaaling(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

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
    return runBlocking(Dispatchers.IO) {
      maalingDAO
          .getMaaling(maalingId)
          ?.let { maaling -> Maaling.findFerdigeTestKoeyringar(maaling, loeysingId) }
          ?.let { ferdigeTestKoeyringar ->
            autoTesterClient.fetchResultat(
                ferdigeTestKoeyringar, AutoTesterClient.ResultatUrls.urlBrot)
          }
          ?.toSingleResult()
          ?.map { it.values.flatten() }
          ?: Result.failure(NullPointerException("Testresultat vart ikkje funne"))
    }
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
      "testresultat" -> hentEllerGenererAggregeringPrTestregel(maalingId)
      "suksesskriterium" -> hentEllerGenererAggregeringPrSuksesskriterium(maalingId)
      "side" -> hentEllerGenererAggregeringPrSide(maalingId)
      else -> throw IllegalArgumentException("Ugyldig aggregeringstype: $aggregeringstype")
    }
  }

  private fun hentEllerGenererAggregeringPrSide(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "side")) {
      val testKoeyringar =
          maalingDAO.getMaaling(maalingId)?.let { maaling ->
            Maaling.findFerdigeTestKoeyringar(maaling)
          }
      testKoeyringar?.forEach { aggregeringService.saveAggregeringSideAutomatisk(it) }
    }
    return aggregeringService.getAggregertResultatSide(maalingId).let { ResponseEntity.ok(it) }
  }

  private fun hentEllerGenererAggregeringPrSuksesskriterium(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "suksesskriterium")) {
      val testKoeyringar =
          maalingDAO.getMaaling(maalingId)?.let { maaling ->
            Maaling.findFerdigeTestKoeyringar(maaling)
          }
      testKoeyringar?.forEach {
        aggregeringService.saveAggregertResultatSuksesskriteriumAutomatisk(it)
      }
    }
    return aggregeringService.getAggregertResultatSuksesskriterium(maalingId).let {
      ResponseEntity.ok(it)
    }
  }

  private fun hentEllerGenererAggregeringPrTestregel(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "testresultat")) {
      logger.info("Aggregering er ikkje generert for måling $maalingId, genererer no")
      val testKoeyringar =
          maalingDAO.getMaaling(maalingId)?.let { maaling ->
            Maaling.findFerdigeTestKoeyringar(maaling)
          }
      testKoeyringar?.forEach { aggregeringService.saveAggregertResultatTestregelAutomatisk(it) }
    }
    return aggregeringService.getAggregertResultatTestregel(maalingId).let { ResponseEntity.ok(it) }
  }

  @GetMapping("{maalingId}/crawlresultat/nettsider")
  fun getCrawlResultatNettsider(
      @PathVariable maalingId: Int,
      @RequestParam loeysingId: Int
  ): ResponseEntity<List<URL>> =
      sideutvalDAO.getSideutvalFraaCrawlResultat(maalingId, loeysingId).map { sideutvalUrl(it) }.let { ResponseEntity.ok(it) }

  @PutMapping("{id}/status")
  fun putNewStatus(@PathVariable id: Int, @RequestBody statusDTO: StatusDTO): ResponseEntity<Any> {
    return runCatching<ResponseEntity<Any>> {
          val maaling = maalingDAO.getMaaling(id)!!
          val newStatus = validateStatus(statusDTO.status).getOrThrow()
          val badRequest = ResponseEntity.badRequest().build<Any>()

          when (maaling) {
            is Maaling.Planlegging -> {
              putNewStatusMaalingPlanlegging(maaling, newStatus, badRequest)
            }
            is Maaling.Kvalitetssikring -> {
              putNewStatusMaalingKvalitetssikring(newStatus, statusDTO, maaling)
            }
            is Maaling.TestingFerdig -> {
              putNewStatusMaalingTestingFerdig(newStatus, statusDTO, maaling, badRequest)
            }
            else -> badRequest
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

  private fun putNewStatusMaalingTestingFerdig(
      newStatus: Status,
      statusDTO: StatusDTO,
      maaling: Maaling.TestingFerdig,
      badRequest: ResponseEntity<Any>
  ): ResponseEntity<Any> {
    val brukar: Brukar = brukarService.getCurrentUser()
    return runBlocking(Dispatchers.IO) {
      when (newStatus) {
        Status.Testing -> restartTesting(statusDTO, maaling, brukar)
        else -> badRequest
      }
    }
  }

  private fun putNewStatusMaalingKvalitetssikring(
      newStatus: Status,
      statusDTO: StatusDTO,
      maaling: Maaling.Kvalitetssikring
  ): ResponseEntity<Any> {
    val brukar = brukarService.getCurrentUser()
    return runBlocking(Dispatchers.IO) {
      when (newStatus) {
        Status.Crawling -> restartCrawling(statusDTO, maaling)
        Status.Testing -> startTesting(maaling, brukar)
      }
    }
  }

  private fun putNewStatusMaalingPlanlegging(
      maaling: Maaling.Planlegging,
      newStatus: Status,
      badRequest: ResponseEntity<Any>
  ): ResponseEntity<Any> {
    return runBlocking(Dispatchers.IO) {
      maaling.crawlParameters.validateParameters()
      when (newStatus) {
        Status.Crawling -> startCrawling(maaling)
        else -> badRequest
      }
    }
  }

  private fun restartCrawling(
      statusDTO: StatusDTO,
      maaling: Maaling.Kvalitetssikring
  ): ResponseEntity<Any> {
      val loeysingIdList =
          validateLoeysingIds(statusDTO)
      val crawlParameters = maalingDAO.getCrawlParameters(maaling.id)
    val updated = crawlerClient.restart(maaling, loeysingIdList, crawlParameters)
    maalingDAO.save(updated).getOrThrow()
    return ResponseEntity.ok().build()
  }

  private suspend fun startCrawling(maaling: Maaling.Planlegging): ResponseEntity<Any> {
    val updated = crawlerClient.start(maaling)
    withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
    return ResponseEntity.ok().build()
  }

  private suspend fun restartTesting(
      statusDTO: StatusDTO,
      maaling: Maaling.TestingFerdig,
      brukar: Brukar
  ): ResponseEntity<Any> {
    return coroutineScope {
      logger.info("Restarter testing for måling ${maaling.id}")
        val loeysingIdList =
            validateLoeysingIds(statusDTO)

        val (retestList, rest) =
          maaling.testKoeyringar.partition { loeysingIdList.contains(it.loeysing.id) }

        val loeysingar = retestList.map { it.loeysing }

      val testKoeyringar = startTesting(maaling.id,loeysingar, brukar)

      val updated =
          Maaling.Testing(
              id = maaling.id,
              navn = maaling.navn,
              datoStart = maaling.datoStart,
              testKoeyringar = rest.plus(testKoeyringar))
      withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
      ResponseEntity.ok().build()
    }
  }

    private fun validateLoeysingIds(statusDTO: StatusDTO): List<Int> {
        val validIds =
            if (statusDTO.loeysingIdList?.isNotEmpty() == true) {
                loeysingsRegisterClient.getMany(statusDTO.loeysingIdList).getOrThrow().map { it.id }
            } else {
                emptyList()
            }
        val loeysingIdList =
            validateIdList(statusDTO.loeysingIdList, validIds, "loeysingIdList").getOrThrow()
        return loeysingIdList
    }

    private suspend fun startTesting(
      maaling: Maaling.Kvalitetssikring,
      brukar: Brukar
  ): ResponseEntity<Any> {
    return coroutineScope {
        val loeysingar = maaling.crawlResultat.filterIsInstance<CrawlResultat.Ferdig>().map { it.loeysing }
      val testKoeyringar =
          startTesting(
              maaling.id, loeysingar, brukar)

      val updated = Maaling.toTesting(maaling, testKoeyringar)
      withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
      ResponseEntity.ok().build()
    }
  }

  data class StatusDTO(val status: String, val loeysingIdList: List<Int>?)

  private suspend fun startTesting(
      maalingId: Int,
      loeysingar: List<Loeysing>,
      brukar: Brukar
  ): List<TestKoeyring> = coroutineScope {
    val testreglar =
        getTestreglarForMaaling(maalingId)


      loeysingar
        .map {
          async {
            val nettsider =
                getNettsider(maalingId, it.id)
              Pair(it, autoTesterClient.startTesting(maalingId, it, testreglar, nettsider))
          }
        }
        .awaitAll()
        .map { (loeysing, result) ->
          result.fold(
              { statusURL -> TestKoeyring.from(loeysing,crawlResultat, statusURL, brukar) },
              { exception ->
                val feilmelding =
                    exception.message
                        ?: "eg klarte ikkje å starte testing for ei løysing, og feilmeldinga manglar"
                TestKoeyring.Feila(loeysing,crawlResultat, Instant.now(), feilmelding, brukar)
              })
        }
  }

    private suspend fun getTestreglarForMaaling(maalingId: Int): List<Testregel> {
        val testreglar =
            withContext(Dispatchers.IO) { testregelDAO.getTestreglarForMaaling(maalingId) }
                .getOrElse {
                    logger.error("Feila ved henting av actregler for måling $maalingId", it)
                    throw it
                }
                .onEach { it.validateTestregel().getOrThrow() }
        return testreglar
    }

    private suspend fun getNettsider(
        maalingId: Int,
        loeysingId: Int,
    ): List<URL> {
        val nettsider =
            withContext(Dispatchers.IO) {
                sideutvalDAO.getSideutvalFraaCrawlResultat(maalingId, loeysingId)
            }.map { sideutvalUrl(it) }

        if (nettsider.isEmpty()) {
            throw RuntimeException(
                "Tomt resultat frå crawling, kan ikkje starte test. maalingId: $maalingId loeysingId: ${loeysingId}"
            )
        }
        return nettsider

    }

    private fun sideutvalUrl(it: SideutvalElementAutomatisk): URL =
        URI(it.sti.sti()).toURL()
}
