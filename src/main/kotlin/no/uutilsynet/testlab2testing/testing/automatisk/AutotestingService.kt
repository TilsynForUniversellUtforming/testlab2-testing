package no.uutilsynet.testlab2testing.testing.automatisk

import java.net.URL
import java.time.Instant
import kotlinx.coroutines.*
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import org.springframework.stereotype.Service

@Service
class AutotestingService(val autoTesterClient: AutoTesterClient, val sideutvalDAO: SideutvalDAO) {

  suspend fun startTesting(
      maalingId: Int,
      brukar: Brukar,
      loeysingar: List<Loeysing>,
      testreglar: List<Testregel>
  ): List<TestKoeyring> = coroutineScope {
    val startTestarStatus = startTestar(loeysingar, maalingId, testreglar)

    startTestarStatus.map { result ->
      result.fold(
          { status ->
            TestKoeyring.from(status.loeysing, status.statusUrl, brukar, status.antallNettsider)
          },
          { exception ->
            val feilmelding =
                exception.message
                    ?: "eg klarte ikkje å starte testing for ei løysing, og feilmeldinga manglar"
            TestKoeyring.Feila(result.getOrThrow().loeysing, Instant.now(), feilmelding, brukar)
          })
    }
  }

  private suspend fun startTestar(
      loeysingar: List<Loeysing>,
      maalingId: Int,
      testreglar: List<Testregel>,
  ): List<Result<AutoTesterClient.AutotestingStatus>> {
    return coroutineScope {
      loeysingar
          .map {
            async {
              autoTesterClient.startTesting(maalingId, testreglar, getNettsider(maalingId, it), it)
            }
          }
          .awaitAll()
    }
  }

  private suspend fun getNettsider(maalingId: Int, loeysing: Loeysing): List<URL> {
    val nettsider =
        withContext(Dispatchers.IO) {
          sideutvalDAO.getCrawlResultatNettsider(maalingId, loeysing.id)
        }
    if (nettsider.isEmpty()) {
      throw RuntimeException(
          "Tomt resultat frå crawling, kan ikkje starte test. maalingId: $maalingId loeysingId: ${loeysing.id}")
    }
    return nettsider
  }
}
