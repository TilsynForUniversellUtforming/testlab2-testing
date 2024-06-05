package no.uutilsynet.testlab2testing.forenkletkontroll

import java.time.Instant
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Service

@Service
class MaalingService(
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testregelDAO: TestregelDAO,
    val utvalDAO: UtvalDAO,
) {

  fun nyMaaling(kontrollId: Int, opprettKontroll: KontrollResource.OpprettKontroll) = runCatching {
    val dto = opprettKontroll.toNyMaaling()
    val navn = validateNamn(dto.navn).getOrThrow()
    val crawlParameters = dto.crawlParameters ?: CrawlParameters()
    crawlParameters.validateParameters()

    val localDateNorway = Instant.now()
    val maalingId =
        maalingDAO.createMaaling(navn, localDateNorway, emptyList(), emptyList(), crawlParameters)
    maalingDAO.updateKontrollId(
        kontrollId,
        maalingId,
    )
  }

  fun nyMaaling(dto: MaalingResource.NyMaalingDTO): Result<Int> = runCatching {
    val navn = validateNamn(dto.navn).getOrThrow()
    val loeysingIdList = validateLoeyingsIdList(dto)
    val utvalId = validatedUtvalId(dto)
    val testregelIdList = validatedTestregeldList(dto)
    val crawlParameters = dto.crawlParameters ?: CrawlParameters()
    crawlParameters.validateParameters()

    val localDateNorway = Instant.now()

    if (utvalId != null) {
      val utval = getUtval(utvalId)
      maalingDAO.createMaaling(navn, localDateNorway, utval, testregelIdList, crawlParameters)
    } else if (loeysingIdList != null) {
      maalingDAO.createMaaling(
          navn, localDateNorway, loeysingIdList, testregelIdList, crawlParameters)
    } else {
      throw IllegalArgumentException("utvalId eller loeysingIdList må være gitt")
    }
  }

  fun updateMaaling(kontroll: Kontroll): Result<Unit> = runCatching {
    val maalingId = maalingDAO.getMaalingIdFromKontrollId(kontroll.id)
    if (maalingId != null) {
      val maalingEdit = kontroll.toMaalingEdit(maalingId)
      maalingDAO.updateMaaling(maalingEdit.toMaaling())
    } else {
      throw IllegalArgumentException("Måling finns ikkje for kontroll")
    }
  }

  fun updateMaaling(dto: EditMaalingDTO): Result<Unit> = runCatching {
    val maalingCopy = dto.toMaaling()
    maalingDAO.updateMaaling(maalingCopy)
  }

  fun deleteKontrollMaaling(kontrollId: Int): Result<Unit> = runCatching {
    val maalingId = maalingDAO.getMaalingIdFromKontrollId(kontrollId)
    if (maalingId != null) {
      return deleteMaaling(maalingId)
    }
  }

  fun deleteMaaling(id: Int): Result<Unit> = runCatching { maalingDAO.deleteMaaling(id) }

  private fun validatedTestregeldList(dto: MaalingResource.NyMaalingDTO): List<Int> {
    val testregelIdList =
        validateIdList(
                dto.testregelIdList,
                testregelDAO.getTestregelList().map { it.id },
                "testregelIdList")
            .getOrThrow()
    return testregelIdList
  }

  private fun validatedUtvalId(dto: MaalingResource.NyMaalingDTO): Int? {
    val utvalIdList = utvalDAO.getUtvalList().getOrDefault(emptyList()).map { it.id }
    val utvalId =
        dto.utvalId?.let { validateIdList(listOf(it), utvalIdList, "utvalId").getOrThrow().first() }
    return utvalId
  }

  private fun validateLoeyingsIdList(dto: MaalingResource.NyMaalingDTO): List<Int>? {
    val loeysingIdList =
        dto.loeysingIdList?.let {
          val loeysingar = loeysingsRegisterClient.getMany(it).getOrThrow()
          validateIdList(dto.loeysingIdList, loeysingar.map { it.id }, "loeysingIdList")
              .getOrThrow()
        }
    return loeysingIdList
  }

  private fun getUtval(utvalId: Int): Utval {
    val utval =
        utvalDAO
            .getUtval(utvalId)
            .mapCatching {
              val loeysingar = loeysingsRegisterClient.getMany(it.loeysingar).getOrThrow()
              Utval(it.id, it.namn, loeysingar, it.oppretta)
            }
            .getOrThrow()
    return utval
  }

  private fun EditMaalingDTO.toMaaling(): Maaling {
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
                ?: emptyList<Loeysing>().also {
                  logger.warn("Måling ${maaling.id} har ikkje løysingar")
                }

        val testregelList =
            this.testregelIdList?.let { idList ->
              testregelDAO.getTestregelList().filter { idList.contains(it.id) }
            }
                ?: emptyList<Testregel>().also {
                  logger.warn("Måling ${maaling.id} har ikkje testreglar")
                }

        maaling.copy(
            navn = navn,
            loeysingList = loeysingList,
            testregelList = testregelList.map { it.toTestregelBase() },
            crawlParameters = this.crawlParameters ?: maaling.crawlParameters)
      }
      is Maaling.Crawling -> maaling.copy(navn = this.navn)
      is Maaling.Testing -> maaling.copy(navn = this.navn)
      is Maaling.TestingFerdig -> maaling.copy(navn = this.navn)
      is Maaling.Kvalitetssikring -> maaling.copy(navn = this.navn)
    }
  }
}
