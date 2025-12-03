package no.uutilsynet.testlab2testing.common

enum class SortOrder {
  asc,
  desc
}

enum class SortParamTestregel {
  side,
  testregel,
  elementPointer,
  elementUtfall
}

data class SortPaginationParams(
    val sortParam: SortParamTestregel,
    val sortOrder: SortOrder,
    val pageNumber: Int,
    val pageSize: Int
)
