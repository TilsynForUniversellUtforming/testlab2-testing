package no.uutilsynet.testlab2testing.kontroll

import org.springframework.stereotype.Service

@Service
class KontrollService(val kontrollDAO: KontrollDAO) {

  fun getResultatForKontroll(kontrollId: Int) {
    val kontroll = getKontrollDB(kontrollId)
  }

  private fun getKontrollDB(kontrollId: Int) =
      kontrollDAO.getKontroller(listOf(kontrollId)).mapCatching { it.first() }.getOrThrow()
}
