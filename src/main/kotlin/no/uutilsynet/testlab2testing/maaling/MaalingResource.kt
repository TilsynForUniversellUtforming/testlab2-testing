package no.uutilsynet.testlab2testing.maaling

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.*
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateStatus
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.dto.Testregel.Companion.validateTestRegel
import no.uutilsynet.testlab2testing.firstMessage
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.maaling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/maalinger")
class MaalingResource(
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testregelDAO: TestregelDAO,
    val utvalDAO: UtvalDAO,
    val crawlerClient: CrawlerClient,
    val autoTesterClient: AutoTesterClient,
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
      runCatching {
            val navn = validateNamn(dto.navn).getOrThrow()
            val loeysingIdList =
                dto.loeysingIdList?.let {
                  val loeysingar = loeysingsRegisterClient.getMany(it).getOrThrow()
                  validateIdList(dto.loeysingIdList, loeysingar.map { it.id }, "loeysingIdList")
                      .getOrThrow()
                }
            val utvalIdList = utvalDAO.getUtvalList().getOrDefault(emptyList()).map { it.id }
            val utvalId =
                dto.utvalId?.let {
                  validateIdList(listOf(it), utvalIdList, "utvalId").getOrThrow().first()
                }
            val testregelIdList =
                validateIdList(
                        dto.testregelIdList,
                        testregelDAO.getTestregelList().map { it.id },
                        "testregelIdList")
                    .getOrThrow()
            val crawlParameters = dto.crawlParameters ?: CrawlParameters()
            crawlParameters.validateParameters()

            val localDateNorway = Instant.now()

            if (utvalId != null) {
              val utval =
                  utvalDAO
                      .getUtval(utvalId)
                      .mapCatching {
                        val loeysingar = loeysingsRegisterClient.getMany(it.loeysingar).getOrThrow()
                        Utval(it.id, it.namn, loeysingar)
                      }
                      .getOrThrow()
              maalingDAO.createMaaling(
                  navn, localDateNorway, utval, testregelIdList, crawlParameters)
            } else if (loeysingIdList != null) {
              maalingDAO.createMaaling(
                  navn, localDateNorway, loeysingIdList, testregelIdList, crawlParameters)
            } else {
              throw IllegalArgumentException("utvalId eller loeysingIdList må være gitt")
            }
          }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception) })

  @PutMapping
  fun updateMaaling(@RequestBody dto: EditMaalingDTO): ResponseEntity<out Any> =
      runCatching {
            val maalingCopy = dto.toMaaling()
            ResponseEntity.ok(maalingDAO.updateMaaling(maalingCopy))
          }
          .getOrElse { exception ->
            logger.error("Feila da vi skulle oppdatere målinga ${dto.id}", exception)
            handleErrors(exception)
          }

  @DeleteMapping("{id}")
  fun deleteMaaling(@PathVariable id: Int): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(maalingDAO.deleteMaaling(id)) }
          .getOrElse { exception -> handleErrors(exception) }

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
      runBlocking(Dispatchers.IO) {
        maalingDAO
            .getMaaling(maalingId)
            ?.let { maaling -> Maaling.findFerdigeTestKoeyringar(maaling, loeysingId) }
            ?.let { ferdigeTestKoeyringar ->
              autoTesterClient.fetchResultat(
                  ferdigeTestKoeyringar, AutoTesterClient.ResultatUrls.urlBrot)
            }
            ?.toSingleResult()
            ?.map { it.values.flatten() }
            ?.fold(
                { testResultatList -> ResponseEntity.ok(testResultatList) },
                { error ->
                  logger.error(
                      "Feila da vi skulle hente fullt resultat for målinga $maalingId", error)
                  ResponseEntity.internalServerError().body(error.firstMessage())
                })
            ?: ResponseEntity.notFound().build()
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
    val aggregeringURL =
        when (aggregeringstype) {
          "testresultat" -> AutoTesterClient.ResultatUrls.urlAggreggeringTR
          "suksesskriterium" -> AutoTesterClient.ResultatUrls.urlAggregeringSK
          "side" -> AutoTesterClient.ResultatUrls.urlAggregeringSide
          else -> throw IllegalArgumentException("Ugyldig aggregeringstype: $aggregeringstype")
        }
    return maalingDAO
        .getMaaling(maalingId)
        ?.let { maaling ->
          runCatching {
            if (maaling is Maaling.TestingFerdig) maaling
            else throw IllegalArgumentException("Måling $maalingId er ikkje ferdig testa")
          }
        }
        ?.map(Maaling.TestingFerdig::testKoeyringar)
        ?.map { testKoeyringar -> testKoeyringar.filterIsInstance<TestKoeyring.Ferdig>() }
        ?.mapCatching { ferdigeTestKoeyringar ->
          runBlocking(Dispatchers.IO) {
            autoTesterClient
                .fetchResultat(ferdigeTestKoeyringar, aggregeringURL)
                .toSingleResult()
                .getOrThrow()
          }
        }
        ?.fold(
            { testResultatList -> ResponseEntity.ok(testResultatList) },
            { error ->
              logger.error("Feila da vi skulle hente aggregering for målinga $maalingId", error)
              ResponseEntity.internalServerError().body(error.firstMessage())
            })
        ?: ResponseEntity.notFound().build()
  }

  @GetMapping("{maalingId}/crawlresultat/nettsider")
  fun getCrawlResultatNettsider(
      @PathVariable maalingId: Int,
      @RequestParam loeysingId: Int
  ): ResponseEntity<List<URL>> =
      maalingDAO.getCrawlResultatNettsider(maalingId, loeysingId).let { ResponseEntity.ok(it) }

  @PutMapping("{id}/status")
  fun putNewStatus(@PathVariable id: Int, @RequestBody statusDTO: StatusDTO): ResponseEntity<Any> {
    return runCatching<ResponseEntity<Any>> {
          val maaling = maalingDAO.getMaaling(id)!!
          val newStatus = validateStatus(statusDTO.status).getOrThrow()
          val badRequest = ResponseEntity.badRequest().build<Any>()

          when (maaling) {
            is Maaling.Planlegging -> {
              runBlocking(Dispatchers.IO) {
                maaling.crawlParameters.validateParameters()
                when (newStatus) {
                  Status.Crawling -> startCrawling(maaling)
                  else -> badRequest
                }
              }
            }
            is Maaling.Kvalitetssikring -> {
              runBlocking(Dispatchers.IO) {
                when (newStatus) {
                  Status.Crawling -> restartCrawling(statusDTO, maaling)
                  Status.Testing -> startTesting(maaling)
                }
              }
            }
            is Maaling.TestingFerdig -> {
              runBlocking(Dispatchers.IO) {
                when (newStatus) {
                  Status.Testing -> restartTesting(statusDTO, maaling)
                  else -> badRequest
                }
              }
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

  private fun restartCrawling(
      statusDTO: StatusDTO,
      maaling: Maaling.Kvalitetssikring
  ): ResponseEntity<Any> {
    val validIds =
        if (statusDTO.loeysingIdList?.isNotEmpty() == true) {
          loeysingsRegisterClient.getMany(statusDTO.loeysingIdList).getOrThrow().map { it.id }
        } else {
          emptyList()
        }
    val loeysingIdList =
        validateIdList(statusDTO.loeysingIdList, validIds, "loeysingIdList").getOrThrow()
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
      maaling: Maaling.TestingFerdig
  ): ResponseEntity<Any> = coroutineScope {
    logger.info("Restarter testing for måling ${maaling.id}")
    val validIds =
        if (statusDTO.loeysingIdList?.isNotEmpty() == true) {
          loeysingsRegisterClient.getMany(statusDTO.loeysingIdList).getOrThrow().map { it.id }
        } else {
          emptyList()
        }
    val loeysingIdList =
        validateIdList(statusDTO.loeysingIdList, validIds, "loeysingIdList").getOrThrow()

    val (retestList, rest) =
        maaling.testKoeyringar.partition { loeysingIdList.contains(it.loeysing.id) }

    val testKoeyringar = startTesting(maaling.id, retestList.map { it.crawlResultat })

    val updated =
        Maaling.Testing(
            id = maaling.id,
            navn = maaling.navn,
            datoStart = maaling.datoStart,
            testKoeyringar = rest.plus(testKoeyringar))
    withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
    ResponseEntity.ok().build()
  }

  private suspend fun startTesting(maaling: Maaling.Kvalitetssikring): ResponseEntity<Any> =
      coroutineScope {
        val testKoeyringar =
            startTesting(maaling.id, maaling.crawlResultat.filterIsInstance<CrawlResultat.Ferdig>())

        val updated = Maaling.toTesting(maaling, testKoeyringar)
        withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
        ResponseEntity.ok().build()
      }

  data class StatusDTO(val status: String, val loeysingIdList: List<Int>?)

  private suspend fun startTesting(
      maalingId: Int,
      crawlResultat: List<CrawlResultat.Ferdig>,
  ): List<TestKoeyring> = coroutineScope {
    val testreglar =
        withContext(Dispatchers.IO) { testregelDAO.getTestreglarForMaaling(maalingId) }
            .getOrElse {
              logger.error("Feila ved henting av actregler for måling $maalingId", it)
              throw it
            }
            .onEach { it.validateTestRegel() }

    crawlResultat
        .map {
          async {
            val nettsider =
                withContext(Dispatchers.IO) {
                  maalingDAO.getCrawlResultatNettsider(maalingId, it.loeysing.id)
                }
            if (nettsider.isEmpty()) {
              throw RuntimeException(
                  "Tomt resultat frå crawling, kan ikkje starte test. maalingId: $maalingId loeysingId: ${it.loeysing.id}")
            }
            Pair(it, autoTesterClient.startTesting(maalingId, it, testreglar, nettsider))
          }
        }
        .awaitAll()
        .map { (crawlResultat, result) ->
          result.fold(
              { statusURL -> TestKoeyring.from(crawlResultat, statusURL) },
              { exception ->
                val feilmelding =
                    exception.message
                        ?: "eg klarte ikkje å starte testing for ei løysing, og feilmeldinga manglar"
                TestKoeyring.Feila(crawlResultat, Instant.now(), feilmelding)
              })
        }
  }

  fun EditMaalingDTO.toMaaling(): Maaling {
    val navn = validateNamn(this.navn).getOrThrow()
    this.crawlParameters?.validateParameters()

    val maaling =
        maalingDAO.getMaaling(this.id) ?: throw IllegalArgumentException("Måling finnes ikkje")
    return when (maaling) {
      is Maaling.Planlegging -> {
        val loeysingList =
            this.loeysingIdList
                ?.let { idList -> loeysingsRegisterClient.getMany(idList) }
                ?.getOrThrow()
                ?: throw IllegalArgumentException("Måling må ha løysingar")

        val testregelList =
            this.testregelIdList?.let { idList ->
              testregelDAO.getTestregelList().filter { idList.contains(it.id) }
            }
                ?: throw IllegalArgumentException("Måling må ha testreglar")

        maaling.copy(
            navn = navn,
            loeysingList = loeysingList,
            testregelList = testregelList,
            crawlParameters = this.crawlParameters ?: maaling.crawlParameters)
      }
      is Maaling.Crawling -> maaling.copy(navn = this.navn)
      is Maaling.Testing -> maaling.copy(navn = this.navn)
      is Maaling.TestingFerdig -> maaling.copy(navn = this.navn)
      is Maaling.Kvalitetssikring -> maaling.copy(navn = this.navn)
    }
  }
}
