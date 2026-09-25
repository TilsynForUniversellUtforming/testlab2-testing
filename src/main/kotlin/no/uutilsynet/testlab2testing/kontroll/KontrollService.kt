package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.kontroll.Kontroll.Testreglar
import no.uutilsynet.testlab2testing.kontroll.KontrollResource.KontrollListItem
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.utval.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable

@Service
class KontrollService(
    private val kontrollDAO: KontrollDAO,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val testregelClient: TestregelClient,
    private val brukarService: BrukarService
) {

  private val logger: Logger = LoggerFactory.getLogger(KontrollService::class.java)

  fun getKontroller(): Result<List<KontrollListItem>> {
    return kontrollDAO.getKontroller().mapCatching { kontrollRows ->
      kontrollRows.map { kontrollDB ->
        val virksomheter = getVirksomheterForKontroll(kontrollDB)

        KontrollListItem(
            kontrollDB.id,
            kontrollDB.tittel,
            kontrollDB.saksbehandler,
            Sakstype.valueOf(kontrollDB.sakstype),
            kontrollDB.arkivreferanse,
            kontrollDB.kontrolltype,
            virksomheter,
            kontrollDB.styringsdataId)
      }
    }
  }

  fun getKontrollAsResult(kontrollId: Int): Result<Kontroll> = runCatching {
    val kontrollDB = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()

    Kontroll(
        kontrollDB.id,
        kontrollDB.kontrolltype,
        kontrollDB.tittel,
        kontrollDB.saksbehandler,
        Sakstype.valueOf(kontrollDB.sakstype),
        kontrollDB.arkivreferanse,
        kontrollDbUtvalToUtval(kontrollDB),
        kontollTestreglarToTestreglar(kontrollDB.testreglar),
        kontrollDB.sideutval)
  }

    fun getKontrollListByUser(userId: String?): Result<List<KontrollListItem>> {

        return kontrollDAO.getKontrollListByUser(getUser(userId)).mapCatching { kontrollRows ->
            kontrollRows.map { kontrollDB ->
                val virksomheter = getVirksomheterForKontroll(kontrollDB)

                KontrollListItem(
                    kontrollDB.id,
                    kontrollDB.tittel,
                    kontrollDB.saksbehandler,
                    Sakstype.valueOf(kontrollDB.sakstype),
                    kontrollDB.arkivreferanse,
                    kontrollDB.kontrolltype,
                    virksomheter,
                    kontrollDB.styringsdataId)
            }
        }
    }

    private fun getUser(userId:String?): Brukar {
        if(userId!= null){
            return brukarService.getBrukarByBrukarnamn(userId)
                ?: throw NoSuchElementException("Bruker med brukarnamn $userId finnes ikkje")
        }
        return brukarService.getCurrentUser()
    }

    fun testingMetadata(@PathVariable kontrollId: Int): KontrollTestingMetadata {
    val kontroll = getKontrollAsResult(kontrollId).getOrThrow()
    val sideutvaltypar = kontrollDAO.getSideutvalType()
    val innholdtypeTestingList = testregelClient.getInnhaldstypeForTesting().getOrThrow()

    val innholdstypeTesting =
        kontroll.testreglar
            ?.testregelIdList
            ?.takeIf { it.isNotEmpty() }
            ?.let { ids ->
              testregelClient
                  .getTestregelListFromIds(ids)
                  .getOrThrow()
                  .mapNotNull { it.innhaldstypeTesting }
                  .mapNotNull { innholdstype ->
                    innholdtypeTestingList.firstOrNull { it.id == innholdstype }
                  }
            }
            ?: emptyList()

    val sideutvalType =
        kontroll.sideutvalList.map { sideutval ->
          sideutvaltypar.first { it.id == sideutval.typeId }
        }

    return KontrollTestingMetadata(innholdstypeTesting, sideutvalType)
  }

  private fun getVirksomheterForKontroll(kontrollDB: KontrollDAO.KontrollDB): List<String> {
    runCatching { getLoeysingarlistFromUtval(kontrollDB.utval).map { it.orgnummer }.distinct() }
        .fold(
            onSuccess = {
              return it
            },
            onFailure = {
              logger.error("Feil ved henting av virksomheter for kontroll ${kontrollDB.id}", it)
              throw it
            })
  }

  private fun getLoeysingarlistFromUtval(utval: KontrollDAO.KontrollDB.Utval?): List<Loeysing> {
    if (utval == null || utval.loeysingar.isEmpty()) return emptyList()
    val idList = utval.loeysingar.map { it.id }
    return loeysingsRegisterClient.getMany(idList).getOrThrow()
  }

  private fun kontrollDbUtvalToUtval(kontroll: KontrollDAO.KontrollDB): Utval? {
    return runCatching {
          kontroll.utval?.let { utval ->
            val loeysingar = getLoeysingarlistFromUtval(utval)
            Utval(utval.id, utval.namn, loeysingar, utval.oppretta)
          }
        }
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error("Feil ved henting av løysingar for utval for kontroll ${kontroll.id}")
              throw it
            })
  }

  private fun kontollTestreglarToTestreglar(
      testreglar: KontrollDAO.KontrollDB.Testreglar?
  ): Testreglar? {
    testreglar?.let {
      return Testreglar(it.regelsettId, it.testregelIdList)
    }
    return null
  }
}
