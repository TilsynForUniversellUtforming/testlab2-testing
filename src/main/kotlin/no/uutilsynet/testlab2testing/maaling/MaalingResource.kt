package no.uutilsynet.testlab2testing.maaling

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.*
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.dto.Testregel.Companion.validateTestRegel
import no.uutilsynet.testlab2testing.firstMessage
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO
import no.uutilsynet.testlab2testing.maaling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/maalinger")
class MaalingResource(
    val maalingDAO: MaalingDAO,
    val loeysingDAO: LoeysingDAO,
    val testregelDAO: TestregelDAO,
    val crawlerClient: CrawlerClient,
    val autoTesterClient: AutoTesterClient,
) {
  data class NyMaalingDTO(
      val navn: String,
      val loeysingIdList: List<Int>,
      val testregelIdList: List<Int>,
      val crawlParameters: CrawlParameters?
  )

  private val logger = LoggerFactory.getLogger(MaalingResource::class.java)

  @PostMapping
  fun nyMaaling(@RequestBody dto: NyMaalingDTO): ResponseEntity<Any> =
      runCatching {
            val navn = validateNavn(dto.navn).getOrThrow()
            val loeysingIdList =
                validateIdList(
                        dto.loeysingIdList, loeysingDAO.getLoeysingIdList(), "loeysingIdList")
                    .getOrThrow()
            val testregelIdList =
                validateIdList(
                        dto.testregelIdList,
                        testregelDAO.getTestregelList().map { it.id },
                        "testregelIdList")
                    .getOrThrow()
            maalingDAO.createMaaling(
                navn, loeysingIdList, testregelIdList, dto.crawlParameters ?: CrawlParameters())
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
          .getOrElse { exception -> handleErrors(exception) }

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
              autoTesterClient.fetchResultat(ferdigeTestKoeyringar, false)
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

  @GetMapping("{maalingId}/testresultat/aggregering")
  fun getAggregering(@PathVariable maalingId: Int): ResponseEntity<Any> =
      maalingDAO
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
                  .fetchResultat(ferdigeTestKoeyringar, true)
                  .toSingleResult()
                  .getOrThrow()
            }
          }
          ?.map { koeyringerMedResultat ->
            TestKoeyring.aggregerPaaTestregel(koeyringerMedResultat, maalingId)
          }
          ?.fold(
              { aggregering ->
                if (aggregering.isEmpty()) {
                  // i dette tilfellet har alle testkjøringene feilet
                  ResponseEntity.notFound().build()
                } else {
                  ResponseEntity.ok(
                      aggregering.sortedBy { it.testregelId.removePrefix("QW-ACT-R").toInt() })
                }
              },
              { error ->
                when (error) {
                  is IllegalArgumentException -> ResponseEntity.badRequest().body(error.message)
                  else -> {
                    logger.error(
                        "Feila da vi skulle hente testresultat for måling $maalingId", error)
                    ResponseEntity.internalServerError().body(error.message)
                  }
                }
              })
          ?: ResponseEntity.notFound().build()

  @GetMapping("{id}/crawlresultat")
  fun getCrawlResultatList(@PathVariable id: Int): ResponseEntity<List<CrawlResultat>> =
      maalingDAO
          .getMaaling(id)
          ?.let { maaling: Maaling ->
            when (maaling) {
              is Maaling.Planlegging -> emptyList()
              is Maaling.Crawling -> maaling.crawlResultat
              is Maaling.Kvalitetssikring -> maaling.crawlResultat
              is Maaling.Testing -> maaling.testKoeyringar.map { it.crawlResultat }
              is Maaling.TestingFerdig -> maaling.testKoeyringar.map { it.crawlResultat }
            }
          }
          ?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

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
    val loeysingIdList =
        validateIdList(statusDTO.loeysingIdList, loeysingDAO.getLoeysingIdList(), "loeysingIdList")
            .getOrThrow()
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

  private suspend fun startTesting(maaling: Maaling.Kvalitetssikring): ResponseEntity<Any> =
      coroutineScope {
        val testReglar =
            withContext(Dispatchers.IO) { testregelDAO.getTestreglarForMaaling(maaling.id) }
                .getOrElse {
                  logger.error("Feila ved henting av actregler for måling ${maaling.id}", it)
                  throw it
                }
                .onEach { it.validateTestRegel() }

        val testKoeyringar =
            maaling.crawlResultat
                .filterIsInstance<CrawlResultat.Ferdig>()
                .map {
                  async { Pair(it, autoTesterClient.startTesting(maaling.id, it, testReglar)) }
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
        val updated = Maaling.toTesting(maaling, testKoeyringar)
        withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
        ResponseEntity.ok().build()
      }

  data class StatusDTO(val status: String, val loeysingIdList: List<Int>?)

  @Scheduled(fixedDelay = 30, timeUnit = SECONDS)
  fun updateStatuses() {
    val alleMaalinger: List<Maaling> =
        maalingDAO.getMaalingListByStatus(listOf(MaalingStatus.crawling, MaalingStatus.testing))

    runCatching {
          val statusCrawling = alleMaalinger.filterIsInstance<Maaling.Crawling>()
          val oppdaterteMaalinger =
              statusCrawling.map { updateCrawlingStatuses(it) }.toSingleResult().getOrThrow()
          oppdaterteMaalinger.map { maalingDAO.save(it) }.toSingleResult().getOrThrow()
          if (statusCrawling.isNotEmpty()) {
            logger.info(
                "oppdaterte status for ${statusCrawling.size} målinger med status `crawling`")
          }
        }
        .getOrElse {
          logger.error("klarte ikke å oppdatere status for målinger med status `crawling`", it)
        }

    runCatching {
          val statusTesting = alleMaalinger.filterIsInstance<Maaling.Testing>()
          val oppdaterteMaalinger = statusTesting.map { updateTestingStatuses(it) }
          oppdaterteMaalinger.map { maalingDAO.save(it) }.toSingleResult().getOrThrow()
          if (statusTesting.isNotEmpty()) {
            logger.info("oppdaterte status for ${statusTesting.size} målinger med status `testing`")
          }
        }
        .getOrElse {
          logger.error(
              "klarte ikke å oppdatere status for målinger med status `testing` i maalingDAO", it)
        }
  }

  private fun updateCrawlingStatuses(maaling: Maaling): Result<Maaling> =
      when (maaling) {
        is Maaling.Crawling -> {
          runCatching {
            val oppdaterteResultater =
                maaling.crawlResultat
                    .filterIsInstance<CrawlResultat.IkkeFerdig>()
                    .map {
                      crawlerClient.getStatus(it).map { newStatus -> updateStatus(it, newStatus) }
                    }
                    .toSingleResult()
                    .getOrThrow()
            val oppdatertMaaling = maaling.copy(crawlResultat = oppdaterteResultater)
            Maaling.toKvalitetssikring(oppdatertMaaling) ?: oppdatertMaaling
          }
        }
        else -> Result.success(maaling)
      }

  private fun updateTestingStatuses(maaling: Maaling.Testing): Maaling {
    val oppdaterteTestKoeyringar =
        maaling.testKoeyringar.map { testKoeyring ->
          autoTesterClient.updateStatus(testKoeyring).getOrElse {
            logger.error(
                "feila da eg forsøkte å hente test status for løysing id ${testKoeyring.loeysing.id} på måling id ${maaling.id}",
                it)
            testKoeyring
          }
        }
    val oppdatertMaaling = maaling.copy(testKoeyringar = oppdaterteTestKoeyringar)
    return Maaling.toTestingFerdig(oppdatertMaaling) ?: oppdatertMaaling
  }

  private fun EditMaalingDTO.toMaaling(): Maaling {
    val navn = validateNavn(this.navn).getOrThrow()
    val maaling =
        maalingDAO.getMaaling(this.id) ?: throw IllegalArgumentException("Måling finnes ikkje")
    return when (maaling) {
      is Maaling.Planlegging -> {
        val loeysingList =
            this.loeysingIdList?.let { ll ->
              loeysingDAO.getLoeysingList().filter { ll.contains(it.id) }
            }
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
