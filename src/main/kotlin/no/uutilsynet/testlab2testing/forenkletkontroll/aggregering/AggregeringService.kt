package no.uutilsynet.testlab2testing.forenkletkontroll.aggregering

import java.net.URI
import no.uutilsynet.testlab2testing.forenkletkontroll.AutoTesterClient
import no.uutilsynet.testlab2testing.forenkletkontroll.TestKoeyring
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AggregeringService(
    val autoTesterClient: AutoTesterClient,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val kravregisterClient: KravregisterClient,
    val testregelDAO: TestregelDAO,
    val aggregeringDAO: AggregeringDAO
) {

  fun saveAggregering(testKoeyring: TestKoeyring.Ferdig) {
    saveAggregertResultatTestregel(testKoeyring)
    saveAggregeringSide(testKoeyring)
    saveAggregertResultatSuksesskriterium(testKoeyring)
  }

  @Transactional
  fun saveAggregertResultatTestregel(testKoeyring: TestKoeyring.Ferdig) {
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringTR?.toURI()

    aggregeringUrl?.let {
      val aggregertResultatTestregel: List<AggregertResultatTestregel> =
          autoTesterClient.fetchResultatAggregering(
              aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggreggeringTR)
              as List<AggregertResultatTestregel>

      aggregertResultatTestregel
          .map { aggregertResultatTestregelToDTO(it) }
          .forEach { aggregeringDAO.createAggregertResultatTestregel(it) }
    }
  }

  @Transactional
  fun saveAggregeringSide(testKoeyring: TestKoeyring.Ferdig) {
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringSide?.toURI()

    aggregeringUrl?.let {
      val aggregeringSide: List<AggregertResultatSide> =
          autoTesterClient.fetchResultatAggregering(
              aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggregeringSide)
              as List<AggregertResultatSide>

      aggregeringSide
          .map { aggregertResultatSide -> aggregerteResultatSideTODTO(aggregertResultatSide) }
          .forEach { aggregeringDAO.createAggregeringSide(it) }
    }
        ?: throw RuntimeException("Aggregering url er null")
  }

  @Transactional
  fun saveAggregertResultatSuksesskriterium(testKoeyring: TestKoeyring.Ferdig) {
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringSK?.toURI()

    aggregeringUrl?.let {
      val aggregertResultatSuksesskriterium: List<AggregertResultatSuksesskriterium> =
          autoTesterClient.fetchResultatAggregering(
              aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggregeringSK)
              as List<AggregertResultatSuksesskriterium>

      aggregertResultatSuksesskriterium
          .map { aggregertResultatSuksesskritieriumToDTO(it) }
          .forEach { aggregeringDAO.createAggregertResultatSuksesskriterium(it) }
    }
        ?: throw RuntimeException("Aggregering url er null")
  }

  fun aggregertResultatTestregelToDTO(
      aggregertResultatTestregel: AggregertResultatTestregel
  ): AggregeringPerTestregelDTO {

    return AggregeringPerTestregelDTO(
        aggregertResultatTestregel.maalingId,
        aggregertResultatTestregel.loeysing.id,
        getTestregelIdFromSchema(aggregertResultatTestregel.testregelId).let { testregelId ->
          testregelId ?: throw RuntimeException("TestregelId er null")
        },
        kravregisterClient
            .getKravIdFromSuksesskritterium(aggregertResultatTestregel.suksesskriterium)
            .getOrThrow(),
        aggregertResultatTestregel.fleireSuksesskriterium.map {
          kravregisterClient.getKravIdFromSuksesskritterium(it).getOrThrow()
        },
        aggregertResultatTestregel.talElementSamsvar,
        aggregertResultatTestregel.talElementBrot,
        aggregertResultatTestregel.talElementVarsel,
        aggregertResultatTestregel.talElementIkkjeForekomst,
        aggregertResultatTestregel.talSiderSamsvar,
        aggregertResultatTestregel.talSiderBrot,
        aggregertResultatTestregel.talSiderIkkjeForekomst,
        aggregertResultatTestregel.testregelGjennomsnittlegSideSamsvarProsent,
        aggregertResultatTestregel.testregelGjennomsnittlegSideBrotProsent)
  }

  fun aggregerteResultatSideTODTO(
      aggregertResultatSide: AggregertResultatSide
  ): AggregeringPerSideDTO {
    return AggregeringPerSideDTO(
        aggregertResultatSide.maalingId,
        aggregertResultatSide.loeysing.id,
        aggregertResultatSide.sideUrl,
        aggregertResultatSide.sideNivaa,
        aggregertResultatSide.gjennomsnittligBruddProsentTR,
        aggregertResultatSide.talElementSamsvar,
        aggregertResultatSide.talElementBrot,
        aggregertResultatSide.talElementVarsel,
        aggregertResultatSide.talElementIkkjeForekomst,
    )
  }

  fun aggregertResultatSuksesskritieriumToDTO(
      aggregertResultatSuksesskriterium: AggregertResultatSuksesskriterium
  ): AggregeringPerSuksesskriteriumDTO {

    return AggregeringPerSuksesskriteriumDTO(
        aggregertResultatSuksesskriterium.maalingId,
        aggregertResultatSuksesskriterium.loeysing.id,
        kravregisterClient
            .getKravIdFromSuksesskritterium(aggregertResultatSuksesskriterium.suksesskriterium)
            .getOrThrow(),
        aggregertResultatSuksesskriterium.talSiderSamsvar,
        aggregertResultatSuksesskriterium.talSiderBrot,
        aggregertResultatSuksesskriterium.talSiderIkkjeForekomst,
    )
  }

  fun dtoToAggregertResultatTestregel(
      aggregeringPerTestregelDTO: AggregeringPerTestregelDTO
  ): AggregertResultatTestregel {
    println(aggregeringPerTestregelDTO)
    aggregeringPerTestregelDTO.fleireSuksesskriterium.forEach { println(it.toInt()) }

    return AggregertResultatTestregel(
        aggregeringPerTestregelDTO.maalingId,
        getLoeysing(aggregeringPerTestregelDTO.loeysingId),
        getTestregelId(aggregeringPerTestregelDTO.testregelId),
        getSuksesskriterium(aggregeringPerTestregelDTO.suksesskriterium),
        aggregeringPerTestregelDTO.fleireSuksesskriterium.map { getSuksesskriterium(it.toInt()) },
        aggregeringPerTestregelDTO.talElementSamsvar,
        aggregeringPerTestregelDTO.talElementBrot,
        aggregeringPerTestregelDTO.talElementVarsel,
        aggregeringPerTestregelDTO.talElementIkkjeForekomst,
        aggregeringPerTestregelDTO.talSiderSamsvar,
        aggregeringPerTestregelDTO.talSiderBrot,
        aggregeringPerTestregelDTO.talSiderIkkjeForekomst,
        aggregeringPerTestregelDTO.testregelGjennomsnittlegSideSamsvarProsent,
        aggregeringPerTestregelDTO.testregelGjennomsnittlegSideBrotProsent)
  }

  fun dtoToAggregertResultatSide(
      aggregeringPerSideDTO: AggregeringPerSideDTO
  ): AggregertResultatSide {
    return AggregertResultatSide(
        aggregeringPerSideDTO.maalingId,
        getLoeysing(aggregeringPerSideDTO.loeysingId),
        aggregeringPerSideDTO.sideUrl,
        aggregeringPerSideDTO.sideNivaa,
        aggregeringPerSideDTO.gjennomsnittligBruddProsentTR,
        aggregeringPerSideDTO.talElementSamsvar,
        aggregeringPerSideDTO.talElementBrot,
        aggregeringPerSideDTO.talElementVarsel,
        aggregeringPerSideDTO.talElementIkkjeForekomst)
  }

  fun dtoTOAggregertResultatSuksesskriterium(
      aggregeringPerSuksesskriteriumDTO: AggregeringPerSuksesskriteriumDTO
  ): AggregertResultatSuksesskriterium {
    return AggregertResultatSuksesskriterium(
        aggregeringPerSuksesskriteriumDTO.maalingId,
        getLoeysing(aggregeringPerSuksesskriteriumDTO.loeysingId),
        getSuksesskriterium(aggregeringPerSuksesskriteriumDTO.suksesskriteriumId),
        aggregeringPerSuksesskriteriumDTO.talSiderSamsvar,
        aggregeringPerSuksesskriteriumDTO.talSiderBrot,
        aggregeringPerSuksesskriteriumDTO.talSiderIkkjeForekomst)
  }

  private fun getSuksesskriterium(suksesskriteriumId: Int) =
      kravregisterClient.getSuksesskriteriumFromKrav(suksesskriteriumId).getOrThrow()

  private fun getLoeysing(loeysingId: Int): Loeysing =
      loeysingsRegisterClient.getLoeysingFromId(loeysingId).getOrThrow()

  fun getTestregelIdFromSchema(testregelKey: String): Int? {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel?.id
    }
  }

  fun getTestregelId(idTestregel: Int): String {
    testregelDAO.getTestregel(idTestregel).let { testregel ->
      return testregel?.testregelId
          ?: throw RuntimeException("Fant ikkje testregel med id $idTestregel")
    }
  }

  fun getAggregertResultatTestregel(maalingId: Int): List<AggregertResultatTestregel> {
    return aggregeringDAO.getAggregertResultatTestregelForMaaling(maalingId).map {
      dtoToAggregertResultatTestregel(it)
    }
  }

  fun getAggregertResultatSide(maalingId: Int): Any {
    return aggregeringDAO.getAggregertResultatSideForMaaling(maalingId).map {
      dtoToAggregertResultatSide(it)
    }
  }

  fun getAggregertResultatSuksesskriterium(maalingId: Int): Any {
    return aggregeringDAO.getAggregertResultatSuksesskriteriumForMaaling(maalingId).map {
      dtoTOAggregertResultatSuksesskriterium(it)
    }
  }

  fun harMaalingLagraAggregering(maalingId: Int, aggregeringstype: String): Boolean {
    return aggregeringDAO.harMaalingLagraAggregering(maalingId, aggregeringstype)
  }
}
