package no.uutilsynet.testlab2testing.wordrapport

import java.net.URI
import java.time.LocalDate
import java.time.ZoneId
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WordRapportBuilder(
    @Autowired val testregelDAO: TestregelDAO,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val kravregisterClient: KravregisterClient
) {

  var rapportNummer: String? = null
  var datoFra: LocalDate = LocalDate.now()
  var datoTil: LocalDate = LocalDate.now()
  var verksemd: String? = null
  var loeysing: Loeysing.Expanded? = null
  var avvik: List<Avvik>? = null
  var kontrollId: Int? = null
  var sider: List<Sideutval> = emptyList()

  fun kontroll(kontroll: KontrollDAO.KontrollDB) = apply {
    rapportNummer = kontroll.arkivreferanse
    datoFra = LocalDate.ofInstant(kontroll.opprettaDato, ZoneId.of("Europe/Oslo"))
    kontrollId = kontroll.id
  }

  fun loeysing(loeysing: Loeysing.Expanded) = apply {
    this.loeysing = loeysing
    this.verksemd = loeysing.verksemd?.namn
    this.sider = siderForKontoll(loeysing.id)
  }

  fun siderForKontoll(loysingId: Int): List<Sideutval> =
      kontrollId?.let { kontrollDAO.findSideutvalByKontrollAndLoeysing(it, listOf(loysingId)) }
          ?: throw IllegalStateException("KontrollId is not set")

  fun testresultat(testresultat: List<ResultatManuellKontroll>) = apply {
    avvik = testresultat.groupBy { it.testregelId }.values.map(::toIndexedAvvik).flatten()
  }

  private fun toIndexedAvvik(testresultat: List<ResultatManuellKontroll>): List<Avvik> {
    return testresultat.mapIndexed { index, resultat -> resultat.toAvvik(index) }.subList(0, 1)
  }

  fun build(): WordRapport {
    return WordRapport(
        rapportNummer = rapportNummer ?: "",
        datoTil = datoFra.toString(),
        datoFra = datoTil.toString(),
        verksemd = verksemd ?: "",
        loeysing = loeysing?.namn ?: "",
        avvik = avvik ?: emptyList())
  }

  fun ResultatManuellKontroll.toAvvik(index: Int): Avvik {

    val testregel =
        testregelDAO.getTestregel(this.testregelId)
            ?: throw IllegalStateException("Testregel not found for id ${this.testregelId}")

    val side = finnSide(this.sideutvalId)
    return Avvik(
        nummer = index + 1,
        resultatId = this.id,
        testregel = testregel.toTestregelRapport(),
        side = Side(side.first, side.second.begrunnelse, side.second.url),
        elementOmtale = this.elementOmtale.toString(),
        elementResultat = this.elementResultat.toString(),
        elementUtfall = this.elementUtfall,
        tema =
            testregel.let {
              testregelDAO.getTemaForTestregel().first { tema -> tema.id == it.tema }.tema
            })
  }

  fun Testregel.toTestregelRapport(): TestregelRapport {
    val krav = kravregisterClient.getWcagKrav(this.kravId)
    return TestregelRapport(
        testregelId = this.id,
        testregelNoekkel = this.testregelId,
        kravId = this.kravId,
        kravTittel = krav.tittel,
        kravUrl = krav.urlRettleiing?.let { URI.create(it) })
  }

  fun finnSide(sideutvalId: Int): Pair<Int, Sideutval> {
    return sider.filter { it.id == sideutvalId }.map { Pair(sider.indexOf(it), it) }.first()
  }
}
