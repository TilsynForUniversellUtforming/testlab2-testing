package no.uutilsynet.testlab2testing.resultat.util

import kotlin.math.min
import no.uutilsynet.testlab2testing.common.SortOrder
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.common.SortParamTestregel
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert

object TestresultatDetaljertListUtils {
    fun List<TestresultatDetaljert>.paginate(sortPaginationParams: SortPaginationParams): List<TestresultatDetaljert> {
        val startIndex = sortPaginationParams.pageNumber * sortPaginationParams.pageSize
        val endIndex = resultSubListEnd(sortPaginationParams, this)
        return this.subList(startIndex, endIndex)
    }

    fun List<TestresultatDetaljert>.sort(sortPaginationParams: SortPaginationParams): List<TestresultatDetaljert> {
        val sorted =
            when (sortPaginationParams.sortParam) {
                SortParamTestregel.side -> this.sortedBy { it.side.toString() }
                SortParamTestregel.testregel -> this.sortedBy { it.testregelNoekkel }
                SortParamTestregel.elementUtfall -> this.sortedBy { it.elementResultat?.name }
                SortParamTestregel.elementPointer -> this.sortedBy { it.elementOmtale?.pointer }
            }
        return if (sortPaginationParams.sortOrder == SortOrder.desc) {
            sorted.reversed()
        } else {
            sorted
        }
    }

    fun resultSubListEnd(sortPaginationParams: SortPaginationParams, resultat: List<TestresultatDetaljert>): Int =
        min((sortPaginationParams.pageNumber + 1) * sortPaginationParams.pageSize, resultat.size)
}
