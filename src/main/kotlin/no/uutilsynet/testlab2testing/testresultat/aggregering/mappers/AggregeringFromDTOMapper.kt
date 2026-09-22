package no.uutilsynet.testlab2testing.testresultat.aggregering.mappers

import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerSideDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerSuksesskriteriumDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatSide
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatSuksesskriterium
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatTestregelAPI
import org.springframework.stereotype.Service

@Service
class AggregeringFromDTOMapper(
    private val testregelCache: TestregelCache,
    private val kravregisterClient: KravregisterClient
) {

  fun dtoToAggregertResultatTestregel(
      aggregeringPerTestregelDB: AggregeringPerTestregelDB,
      loeysingList: List<Loeysing>
  ): AggregertResultatTestregelAPI {

    val id = aggregeringPerTestregelDB.maalingId ?: aggregeringPerTestregelDB.testgrunnlagId

    return AggregertResultatTestregelAPI(
        id,
        getLoeysing(aggregeringPerTestregelDB.loeysingId, loeysingList),
        resolveTestregelId(aggregeringPerTestregelDB.testregelId),
        getSuksesskriterium(aggregeringPerTestregelDB.suksesskriterium),
        aggregeringPerTestregelDB.talElementSamsvar,
        aggregeringPerTestregelDB.talElementBrot,
        aggregeringPerTestregelDB.talElementVarsel,
        aggregeringPerTestregelDB.talElementIkkjeForekomst,
        aggregeringPerTestregelDB.talSiderSamsvar,
        aggregeringPerTestregelDB.talSiderBrot,
        aggregeringPerTestregelDB.talSiderIkkjeForekomst,
        aggregeringPerTestregelDB.testregelGjennomsnittlegSideSamsvarProsent,
        aggregeringPerTestregelDB.testregelGjennomsnittlegSideBrotProsent)
  }

  fun dtoToAggregertResultatSide(
      aggregeringPerSideDB: AggregeringPerSideDB,
      loeysingList: List<Loeysing>
  ): AggregertResultatSide {
    return AggregertResultatSide(
        aggregeringPerSideDB.maalingId ?: aggregeringPerSideDB.testgrunnlagId,
        getLoeysing(aggregeringPerSideDB.loeysingId, loeysingList),
        aggregeringPerSideDB.sideUrl,
        aggregeringPerSideDB.sideNivaa,
        aggregeringPerSideDB.gjennomsnittligBruddProsentTR,
        aggregeringPerSideDB.talElementSamsvar,
        aggregeringPerSideDB.talElementBrot,
        aggregeringPerSideDB.talElementVarsel,
        aggregeringPerSideDB.talElementIkkjeForekomst)
  }

  fun dtoTOAggregertResultatSuksesskriterium(
      aggregeringPerSuksesskriteriumDB: AggregeringPerSuksesskriteriumDB,
      loeysingList: List<Loeysing>
  ): AggregertResultatSuksesskriterium {
    return AggregertResultatSuksesskriterium(
        aggregeringPerSuksesskriteriumDB.maalingId
            ?: aggregeringPerSuksesskriteriumDB.testgrunnlagId,
        getLoeysing(aggregeringPerSuksesskriteriumDB.loeysingId, loeysingList),
        getSuksesskriterium(aggregeringPerSuksesskriteriumDB.suksesskriteriumId),
        aggregeringPerSuksesskriteriumDB.talSiderSamsvar,
        aggregeringPerSuksesskriteriumDB.talSiderBrot,
        aggregeringPerSuksesskriteriumDB.talSiderIkkjeForekomst)
  }

  private fun getSuksesskriterium(suksesskriteriumId: Int) =
      kravregisterClient.getSuksesskriteriumFromKrav(suksesskriteriumId)

  private fun getLoeysing(loeysingId: Int, loeysingList: List<Loeysing>): Loeysing =
      loeysingList.firstOrNull { it.id == loeysingId }
          ?: throw NoSuchElementException("Fant ikkje loeysing med id $loeysingId")

  fun getTestregel(testregelId: Int): TestregelAggregate {
    return testregelCache.getTestregelById(testregelId)
  }

  private fun resolveTestregelId(testregelId: Int): String {
    return runCatching { getTestregel(testregelId).testregelId }
        .getOrElse { testregelId.toString() }
  }
}
