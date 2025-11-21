package no.uutilsynet.testlab2testing.testregel

import io.micrometer.observation.annotation.Observed
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.model.InnhaldstypeTesting
import no.uutilsynet.testlab2testing.testregel.model.Tema
import no.uutilsynet.testlab2testing.testregel.model.Testobjekt
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelInit
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.springframework.stereotype.Service

@Service
class TestregelService(
    private val testregelDAO: TestregelDAO,
    private val kravregisterClient: KravregisterClient
) {

  fun getTestregel(testregelId: Int): Testregel =
      testregelDAO.getTestregel(testregelId)
          ?: throw IllegalArgumentException("Fant ikkje testregel med id $testregelId")

    fun getTestregelListFromIds(testregelIdList: List<Int>): List<Testregel> {
    return testregelDAO.getMany(testregelIdList)
  }

  fun getTestregelList(): List<Testregel> {
    return testregelDAO.getTestregelList()
  }

  fun createTema(tema: String): Int {
    return testregelDAO.createTema(tema)
  }

  fun createInnhaldstypeForTesting(innhaldstype: String): Int {
    return testregelDAO.createInnholdstypeTesting(innhaldstype)
  }

  fun createTestregel(testregelInit: TestregelInit): Int {
    return testregelDAO.createTestregel(testregelInit)
  }

  fun getTemaForTestregel(): List<Tema> {
    return testregelDAO.getTemaForTestregel()
  }

  fun getInnhaldstypeForTesting(): List<InnhaldstypeTesting> {
    return testregelDAO.getInnhaldstypeForTesting()
  }

  fun getTestobjekt(): List<Testobjekt> {
    return testregelDAO.getTestobjekt()
  }

  fun updateTestregel(testregel: Testregel): Int {
    return testregelDAO.updateTestregel(testregel)
  }

  fun deleteTestregel(testregelId: Int): Int {
    return testregelDAO.deleteTestregel(testregelId)
  }

  @Observed(name = "testregelservice.gettestregelkravlist")
  fun getTestregelKravList(): List<TestregelKrav> {
    val testregler = testregelDAO.getTestregelList()
    val kravMap = kravregisterClient.listKrav().associateBy { it.id }

    return testregler.map { testregel ->
      val krav =
          kravMap[testregel.kravId]
              ?: throw IllegalArgumentException("Fant ikkje krav med id ${testregel.kravId}")
      TestregelKrav(testregel, krav)
    }
  }
}
