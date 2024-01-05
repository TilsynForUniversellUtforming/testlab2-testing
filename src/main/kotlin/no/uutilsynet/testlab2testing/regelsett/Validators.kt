package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelModus

fun validateRegelsettTestreglar(
    testregelIdList: List<Int>,
    type: TestregelModus,
    existingTestregelList: List<Testregel>
): Result<List<Int>> {
  val testregelIdsOfType = existingTestregelList.filter { it.modus == type }.map { it.id }

  return validateIdList(testregelIdList, testregelIdsOfType, "testregelIdList")
}
