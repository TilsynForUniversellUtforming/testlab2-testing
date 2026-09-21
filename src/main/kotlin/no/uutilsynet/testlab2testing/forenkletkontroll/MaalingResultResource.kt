package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2.constants.TimeConstants.Companion.ZONEID_OSLO
import java.time.LocalDate
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.resultat.LoeysingResultat
import no.uutilsynet.testlab2testing.resultat.ResultatService
import no.uutilsynet.testlab2testing.testing.automatisk.TestKoeyring
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatTestregelAPI
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.math.roundToInt

@RestController
@RequestMapping("v1/maalinger/testresultat")
class MaalingResultResource(
    private val maalingService: MaalingService,
    private val resultatService: ResultatService,
    private val maalingDAO: MaalingDAO,
    private val aggregeringService: AggregeringService
) {

    @GetMapping("{maalingId}")
  fun getTestresultatForMaaling(@PathVariable maalingId: Int): List<Testresult> {
    val kontrollId = maalingService.getKontrollIdForMaaling(maalingId)
      val maaling = maalingDAO.getMaaling(maalingId)
    val testresultat = resultatService.getKontrollResultatByKontrollId(kontrollId).flatMap { it.loeysingar }
      val resultMap = testresultat.groupBy { it.loeysingId }

      return if(maaling is Maaling.TestingFerdig) {
        val testkoeyringar = maaling.testKoeyringar
          testkoeyringar.map {
              val results = resultMap[it.loeysing.id] ?: emptyList()
              val overallCompliancePercent =
                  calculateOverallCompliancePercentage(results)
              Testresult(
                  loeysing = it.loeysing,
                  tilstand = testkoeyringStatus(it),
                  sistOppdatert = LocalDate.ofInstant(it.sistOppdatert, ZONEID_OSLO),
                  framgang = testKoeyringFramgang(it),
                  compliancePercent = overallCompliancePercent,
                  antalSider = testKoeyringAntalSider(it))
          }
      } else {
          emptyList()
      }
  }


    @GetMapping("{maalingId}/loeysing/{loeysingId}")
    fun getTestresultatForLoeysing(
        @PathVariable maalingId: Int,
        @PathVariable loeysingId: Int
    ): List<AggregertResultatTestregelAPI> {
        return aggregeringService.getAggregertResultatTestregel(maalingId, loeysingId)
    }

    private fun calculateOverallCompliancePercentage(
        results: List<LoeysingResultat>
    ): Int? {
        val compliancePercentsForAverage = calculateCompliancePercentArray(results)
        val overallCompliancePercent =
            if (compliancePercentsForAverage.isEmpty()) null
            else compliancePercentsForAverage.average().roundToInt()
        return overallCompliancePercent
    }

    private fun calculateCompliancePercentArray(results: List<LoeysingResultat>): List<Int> {
        val compliancePercentsForAverage = mutableListOf<Int>()


        results.forEach { result ->
            val compliancePercent = result.score.times(100).roundToInt()
            if (result.talElementBrot != 0 || result.talElementSamsvar != 0) {
                compliancePercentsForAverage.add(compliancePercent)
            }
        }
        return compliancePercentsForAverage.toList()
    }

    fun testkoeyringStatus(testkoeyring: TestKoeyring): JobStatus {
        return when (testkoeyring) {
            is TestKoeyring.Ferdig -> JobStatus.ferdig
            is TestKoeyring.Feila ->  JobStatus.feila
            is TestKoeyring.Starta -> JobStatus.starta
            is TestKoeyring.IkkjeStarta -> JobStatus.ikkje_starta
        }}

        fun testKoeyringFramgang(testkoeyring: TestKoeyring): Framgang? {
            return when (testkoeyring) {
                is TestKoeyring.Starta -> testkoeyring.framgang
                else -> null
            }
        }

    fun testKoeyringAntalSider(testkoeyring: TestKoeyring): Int? {
        return when (testkoeyring) {
            is TestKoeyring.IkkjeStarta -> testkoeyring.antallNettsider
            is TestKoeyring.Starta -> testkoeyring.antallNettsider
            is TestKoeyring.Ferdig -> testkoeyring.antallNettsider
            is TestKoeyring.Feila -> null
        }
    }
}

data class Testresult(
    val loeysing: Loeysing,
    val tilstand: JobStatus,
    val sistOppdatert: LocalDate,
    val framgang: Framgang?,
    val compliancePercent: Int?,
    val antalSider: Int?)

enum class JobStatus {
  ikkje_starta,
  starta,
  ferdig,
  feila,
}
