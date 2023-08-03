package no.uutilsynet.testlab2testing.maaling

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit.SECONDS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateStatus
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.dto.Testregel.Companion.validateTestRegel
import no.uutilsynet.testlab2testing.firstMessage
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.maaling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
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
@RequestMapping("v1/maalinger")
class MaalingResource(
    val maalingDAO: MaalingDAO,
    val loeysingDAO: LoeysingDAO,
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
                  validateIdList(
                          dto.loeysingIdList, loeysingDAO.getLoeysingIdList(), "loeysingIdList")
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

            val localDateNorway = LocalDate.now(ZoneId.of("Europe/Oslo"))

            if (utvalId != null) {
              val utval = utvalDAO.getUtval(utvalId).getOrThrow()
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

  private suspend fun restartTesting(
      statusDTO: StatusDTO,
      maaling: Maaling.TestingFerdig
  ): ResponseEntity<Any> = coroutineScope {
    val loeysingIdList =
        validateIdList(statusDTO.loeysingIdList, loeysingDAO.getLoeysingIdList(), "loeysingIdList")
            .getOrThrow()

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
        .map { async { Pair(it, autoTesterClient.startTesting(maalingId, it, testreglar)) } }
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

  private fun EditMaalingDTO.toMaaling(): Maaling {
    val navn = validateNamn(this.navn).getOrThrow()
    this.crawlParameters?.validateParameters()

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
