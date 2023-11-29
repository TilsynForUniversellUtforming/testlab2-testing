package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelType

fun validateRegelsettTestreglar(
    testregelIdList: List<Int>,
    type: TestregelType,
    existingTestregelList: List<Testregel>
): Result<List<Int>> {
  val testregelIdsOfType = existingTestregelList.filter { it.type == type }.map { it.id }

  return validateIdList(testregelIdList, testregelIdsOfType, "testregelIdList")
}
