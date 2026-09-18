package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URL
import java.time.LocalDate
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/maalinger/testresultat")
class MaalingResultResource(
    private val maalingService: MaalingService,
    private val resultatService: ResultatService
) {
  fun getTestresultatForMaaling(maalingId: Int, loeysingId: Int?) {
    val kontrollId = maalingService.getKontrollIdForMaaling(maalingId)
    val testresultat = resultatService.getKontrollResultatByKontrollId(kontrollId)
    val testkoeyringar = maalingService.getTestkoeyringar(maalingId)
//    testkoeyringar.map {
//      val loeysing = maalingService.getLoeysingForTestkoeyring(it.id)
//      Testresult(
//          loeysing = loeysing,
//          tilstand = it.tilstand,
//          sistOppdatert = it.sistOppdatert,
//          framgang = it.framgang,
//          compliancePercent = it.compliancePercent,
//          antalSider = it.antalSider)
//    }
    //        val testresultat = maalingService.getTestresultatForMaaling(maalingId, loeysingId)
    //        return ResponseEntity.ok(testresultat)
  }
}

data class Testresult(
    val loeysing: Loeysing,
    val tilstand: JobStatus,
    val sistOppdatert: LocalDate,
    val framgang: Framgang?,
    val compliancePercent: Int?,
    val antalSider: Int?,
)

data class LoeysingVerksemd(
    val id: Int,
    val namn: String,
    val url: URL,
    val orgnummer: String,
    val verksemdNamn: String,
)

enum class JobStatus {
  ikkje_starta,
  starta,
  ferdig,
  feila,
}
