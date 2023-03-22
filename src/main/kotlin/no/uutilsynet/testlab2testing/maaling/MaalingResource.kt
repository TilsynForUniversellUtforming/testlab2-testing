package no.uutilsynet.testlab2testing.maaling

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS
import no.uutilsynet.testlab2testing.Features
import no.uutilsynet.testlab2testing.dto.Loeysing
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/maalinger")
class MaalingResource(
    val maalingDAO: MaalingDAO,
    val crawlerClient: CrawlerClient,
    val autoTesterClient: AutoTesterClient,
    val features: Features
) {
  data class NyMaalingDTO(val navn: String, val loeysingIdList: List<Int>)
  class InvalidUrlException(message: String) : Exception(message)

  private val logger = LoggerFactory.getLogger(MaalingResource::class.java)

  @PostMapping
  fun nyMaaling(@RequestBody dto: NyMaalingDTO): ResponseEntity<Any> =
      runCatching {
            val navn = validateNavn(dto.navn).getOrThrow()
            maalingDAO.createMaaling(navn, dto.loeysingIdList)
          }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception) })

  @GetMapping
  fun list(): List<Maaling> {
    return maalingDAO.getMaalingList().map { applyFeatures(it, features) }
  }

  @GetMapping("{id}")
  fun getMaaling(@PathVariable id: Int): ResponseEntity<Maaling> =
      maalingDAO.getMaaling(id)?.let { applyFeatures(it, features) }?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

  @PutMapping("{id}/status")
  fun putNewStatus(@PathVariable id: Int, @RequestBody statusDTO: StatusDTO): ResponseEntity<Any> {
    return runCatching<ResponseEntity<Any>> {
          val maaling = maalingDAO.getMaaling(id)!!
          val newStatus = validateStatus(statusDTO.status).getOrThrow()
          when {
            newStatus == Status.Crawling && maaling is Maaling.Planlegging -> {
              val updated = crawlerClient.start(maaling)
              maalingDAO.save(updated).getOrThrow()
              ResponseEntity.ok().build()
            }
            newStatus == Status.Crawling && maaling is Maaling.Kvalitetssikring -> {
              val loeysingIdList = validateLoeysingIdList(statusDTO.loeysingIdList).getOrThrow()
              val updated = crawlerClient.restart(maaling, loeysingIdList)
              maalingDAO.save(updated).getOrThrow()
              ResponseEntity.ok().build()
            }
            newStatus == Status.Testing && maaling is Maaling.Kvalitetssikring -> {
              if (!features.startTesting) {
                ResponseEntity.badRequest().build()
              } else {
                val testKoeyringar =
                    maaling.crawlResultat
                        .filterIsInstance<CrawlResultat.Ferdig>()
                        .map {
                          val result = autoTesterClient.startTesting(maaling.id, it)
                          Pair(it, result)
                        }
                        .map { (crawlResultat, result) ->
                          result.fold(
                              { statusURL -> TestKoeyring.from(crawlResultat.loeysing, statusURL) },
                              { exception ->
                                val feilmelding =
                                    exception.message
                                        ?: "eg klarte ikkje å starte testing for ei løysing, og feilmeldinga manglar"
                                TestKoeyring.Feila(
                                    crawlResultat.loeysing, Instant.now(), feilmelding)
                              })
                        }
                val updated = Maaling.toTesting(maaling, testKoeyringar)
                maalingDAO.save(updated).getOrThrow()
                ResponseEntity.ok().build()
              }
            }
            else -> {
              ResponseEntity.badRequest().build()
            }
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

  data class StatusDTO(val status: String, val loeysingIdList: List<Int>?)

  @GetMapping("loeysingar") fun getLoeysingarList(): List<Loeysing> = maalingDAO.getLoeysingarList()

  @Scheduled(fixedDelay = 30, timeUnit = SECONDS)
  fun updateStatuses() {
    val alleMaalinger = maalingDAO.getMaalingList()

    runCatching {
          val statusCrawling = alleMaalinger.filterIsInstance<Maaling.Crawling>()
          val oppdaterteMaalinger =
              statusCrawling.map { updateCrawlingStatuses(it) }.toSingleResult().getOrThrow()
          oppdaterteMaalinger.map { maalingDAO.save(it) }.toSingleResult().getOrThrow()
          logger.info("oppdaterte status for ${statusCrawling.size} målinger med status `crawling`")
        }
        .getOrElse {
          logger.error("klarte ikke å oppdatere status for målinger med status `crawling`", it)
        }

    runCatching {
          val statusTesting = alleMaalinger.filterIsInstance<Maaling.Testing>()
          val oppdaterteMaalinger =
              statusTesting.map { updateTestingStatuses(it) }.toSingleResult().getOrThrow()
          oppdaterteMaalinger.map { maalingDAO.save(it) }.toSingleResult().getOrThrow()
          logger.info("oppdaterte status for ${statusTesting.size} målinger med status `testing`")
        }
        .getOrElse {
          logger.error("klarte ikke å oppdatere status for målinger med status `testing`", it)
        }
  }

  private fun updateCrawlingStatuses(maaling: Maaling): Result<Maaling> =
      when (maaling) {
        is Maaling.Crawling -> {
          runCatching {
            val oppdaterteResultater =
                maaling.crawlResultat
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

  private fun updateTestingStatuses(maaling: Maaling.Testing): Result<Maaling> = runCatching {
    val oppdaterteTestKoeyringar =
        maaling.testKoeyringar
            .map { testKoeyring -> autoTesterClient.updateStatus(testKoeyring) }
            .toSingleResult()
            .getOrThrow()
    maaling.copy(testKoeyringar = oppdaterteTestKoeyringar)
  }

  private fun handleErrors(exception: Throwable): ResponseEntity<Any> =
      when (exception) {
        is InvalidUrlException,
        is NullPointerException -> ResponseEntity.badRequest().body(exception.message)
        else -> ResponseEntity.internalServerError().body(exception.message)
      }
}

fun applyFeatures(maaling: Maaling, features: Features): Maaling =
    when (maaling) {
      is Maaling.Kvalitetssikring -> {
        if (!features.startTesting) {
          val aksjonerUtenStartTesting = maaling.aksjoner.filterNot { it is Aksjon.StartTesting }
          Maaling.Kvalitetssikring(
              maaling.id, maaling.navn, maaling.crawlResultat, aksjoner = aksjonerUtenStartTesting)
        } else {
          maaling
        }
      }
      else -> maaling
    }

private fun <E> List<Result<E>>.toSingleResult(): Result<List<E>> = runCatching {
  this.map { it.getOrThrow() }
}
