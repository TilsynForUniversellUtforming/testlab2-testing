package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import org.springframework.stereotype.Service

@Service
class TestregelService(val testregelDAO: TestregelDAO, val kravregisterClient: KravregisterClient) {

  fun getKravWcag2x(testregelId: Int): KravWcag2x {
    val krav = getTestregel(testregelId).kravId.let { kravregisterClient.getWcagKrav(it) }
    return krav
  }

  fun getTestregel(testregelId: Int): Testregel =
      testregelDAO.getTestregel(testregelId)
          ?: throw IllegalArgumentException("Fant ikkje testregel med id $testregelId")

  fun getTestregelFromSchema(testregelKey: String): Testregel {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel
          ?: throw RuntimeException("Fant ikke testregel med testregelId $testregelKey")
    }
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

  fun getMany(testregelIdList: List<Int>): List<Testregel> {
    return testregelDAO.getMany(testregelIdList)
  }

  fun getTestregelByTestregelId(testregelKey: String): Testregel {
    return testregelDAO.getTestregelByTestregelId(testregelKey)
        ?: throw RuntimeException("Fant ikkje testregel for testregelId $testregelKey")
  }

  fun getTemaForTestregel(): List<Tema> {
    return testregelDAO.getTemaForTestregel()
  }

  fun getTestregelForKrav(kravId: Int): List<Testregel> {
    return testregelDAO.getTestregelForKrav(kravId)
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
}
