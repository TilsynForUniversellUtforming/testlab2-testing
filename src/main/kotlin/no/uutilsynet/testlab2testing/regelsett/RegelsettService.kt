package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel.Companion.toTestregelBase
import org.springframework.stereotype.Service

@Service
class RegelsettService(
    private val regelsettDAO: RegelsettDAO,
    private val testregelClient: TestregelClient
) {

  fun getRegelsett(regelsettId: Int): Regelsett {
    return regelsettDAO.getRegelsett(regelsettId)?.toRegelsett()
        ?: throw IllegalArgumentException("Fant ikkje regelsett med id $regelsettId")
  }

  fun getRegelsettResponse(int: Int): RegelsettResponse? {
    return getRegelsett(int).let { toRegelsettResponse(it) }
  }

  fun getRegelsettTestreglarList(includeInactive: Boolean): List<Regelsett> =
      regelsettDAO.getRegelsettBaseList(includeInactive).map { it.toRegelsett() }

  fun RegelsettBase.toRegelsett(): Regelsett {
    val testregelIds = regelsettDAO.getTestregelIdListForRegelsett(this.id)
    val testregelList = testregelClient.getTestregelListFromIds(testregelIds).getOrThrow()

      return Regelsett(
        this.id, this.namn, this.modus, this.standard, testregelList.map { it.toTestregelBase() })
  }

  fun toRegelsettResponse(regelsett: Regelsett): RegelsettResponse {
    return RegelsettResponse(
        regelsett.id, regelsett.namn, regelsett.modus, regelsett.standard, regelsett.testregelList)
  }
}
