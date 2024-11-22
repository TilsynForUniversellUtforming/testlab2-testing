package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.resultat.ResultatKrav
import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatTema

data class ResultatOversiktLoeysingEkstern(
    val loeysingNamn: String,
    val typeKontroll: Kontrolltype,
    val kontrollNamn: String,
    val kravTittel: String,
    val kravId: Int,
    val score: Double?,
    val talTestaElement: Int,
    val talElementBrot: Int,
    val talElementSamsvar: Int,
)

fun ResultatOversiktLoeysing.toResultatOversiktLoeysingEkstern() =
    ResultatOversiktLoeysingEkstern(
        loeysingNamn = this.loeysingNamn,
        typeKontroll = this.typeKontroll,
        kontrollNamn = this.kontrollNamn,
        kravTittel = this.kravTittel,
        kravId = this.kravId,
        score = this.score,
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
        talElementIkkjeForekomst = this.talElementIkkjeForekomst)
