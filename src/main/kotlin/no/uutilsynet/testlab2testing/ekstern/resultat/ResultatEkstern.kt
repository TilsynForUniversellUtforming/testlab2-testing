package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.resultat.ResultatKrav
import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatTema

data class ResultatOversiktLoeysingEkstern(
  val loeysingNamn: String,
  val typeKontroll: Kontroll.Kontrolltype,
  val score: Double?,
  val kravTittel: String,
  val talTestaElement: Int,
  val talElementBrot: Int,
  val talElementSamsvar: Int,
)

fun ResultatOversiktLoeysing.toResultatOversiktLoeysingEkstern() =
  ResultatOversiktLoeysingEkstern(
    loeysingNamn = this.loeysingNamn,
    typeKontroll = this.typeKontroll,
    score = this.score,
    kravTittel = this.kravTittel,
    talTestaElement = this.talTestaElement,
    talElementBrot = this.talElementBrot,
    talElementSamsvar = this.talElementSamsvar,
  )

data class ResultatTemaEkstern(
  val temaNamn: String,
  val score: Int,
  val talTestaElement: Int,
  val talElementBrot: Int,
  val talElementSamsvar: Int,
  val talVarsel: Int,
  val talElementIkkjeForekomst: Int,
)

fun ResultatTema.toResultatTemaEkstern() =
  ResultatTemaEkstern(
    temaNamn = this.temaNamn,
    score = this.score,
    talTestaElement = this.talTestaElement,
    talElementBrot = this.talElementBrot,
    talElementSamsvar = this.talElementSamsvar,
    talVarsel = this.talVarsel,
    talElementIkkjeForekomst = this.talElementIkkjeForekomst,
  )

data class ResultatKravEkstern(
  val suksesskriterium: String,
  val score: Int,
  val talTestaElement: Int,
  val talElementBrot: Int,
  val talElementSamsvar: Int,
  val talElementVarsel: Int,
  val talElementIkkjeForekomst: Int,
)

fun ResultatKrav.toResultatKravEkstern() =
  ResultatKravEkstern(
    suksesskriterium = this.suksesskriterium,
    score = this.score,
    talTestaElement = this.talTestaElement,
    talElementBrot = this.talElementBrot,
    talElementSamsvar = this.talElementSamsvar,
    talElementVarsel = this.talElementVarsel,
    talElementIkkjeForekomst = this.talElementIkkjeForekomst
  )
