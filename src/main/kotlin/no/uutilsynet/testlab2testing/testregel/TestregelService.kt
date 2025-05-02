package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import org.springframework.stereotype.Service

@Service
class TestregelService(val testregelDAO: TestregelDAO, val kravregisterClient: KravregisterClient) {

  fun getKravWcag2x(testregelId: Int): KravWcag2x {
    val krav =
        getTestregel(testregelId)?.kravId?.let { kravregisterClient.getWcagKrav(it) }
            ?: throw RuntimeException("Fant ikke krav for testregel $testregelId")
    return krav
  }


  private fun getTestregel(testregelId: Int) = testregelDAO.getTestregel(testregelId)

  fun getSuksesskriteriumFromKrav(kravId: Int) =
      kravregisterClient.getSuksesskriteriumFromKrav(kravId)

    fun getTestreglarMedMetadata(): List<TestregelWithMetadata> {
        val testreglar = testregelDAO.getTestregelList()
        val temaListe = testregelDAO.getTemaForTestregel()
        val testobjektListe = testregelDAO.getTestobjekt()
        val kravListe = kravregisterClient.getKravListe()
        val innholdstypeListe = testregelDAO.getInnhaldstypeForTesting()

        return testreglar.map { testregel ->
            val kravId = testregel.kravId
            val krav = kravListe.find { it.id == kravId } ?: throw RuntimeException("Fant ikke krav for testregel $kravId")
            val tema = temaListe.find { it.id == testregel.tema } ?: throw RuntimeException("Fant ikke tema for testregel ${testregel.id}")
            val testobjekt =
                testobjektListe.find { it.id == testregel.testobjekt } ?: throw RuntimeException("Fant ikke testobjekt for testregel ${testregel.id}")
            val innhaldstypeTesting = innholdstypeListe.find { it.id == testregel.innhaldstypeTesting }

            TestregelWithMetadata(
                id = testregel.id,
                testregelId = testregel.testregelId,
               versjon = testregel.versjon,
              namn = testregel.namn,
               krav = krav,
                status = testregel.status,
                datoSistEndra = testregel.datoSistEndra,
                type = testregel.type,
                modus = testregel.modus,
                spraak = testregel.spraak,
                tema = tema,
                testobjekt = testobjekt,
                kravTilSamsvar = testregel.kravTilSamsvar,
                testregelSchema = testregel.testregelSchema,
                innhaldstypeTesting = innhaldstypeTesting,


            )
        }
    }

   /* fun getTestreglarWithMetadata(): List<TestregelWithMetadata> {
        val testreglar =  testregelDAO.getTestregelList()
    }*/
}
