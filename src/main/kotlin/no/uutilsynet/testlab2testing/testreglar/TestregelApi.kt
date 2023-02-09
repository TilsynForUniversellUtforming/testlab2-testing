package no.uutilsynet.testlab2testing.testreglar

import no.uutilsynet.testlab2testing.dto.Regelsett
import no.uutilsynet.testlab2testing.dto.Testregel

interface TestregelApi {
  fun listTestreglar(): List<Testregel>
  fun listRegelsett(): List<Regelsett>
  fun createTestregel(testregel: Testregel): List<Testregel>
  fun createRegelsett(regelsettRequest: RegelsettRequest): List<Regelsett>
  fun updateTestregel(testregel: Testregel): Testregel
  fun updateRegelsett(regelsett: Regelsett): Regelsett
  fun deleteTestregel(id: Int)
  fun deleteRegelsett(id: Int)
}
