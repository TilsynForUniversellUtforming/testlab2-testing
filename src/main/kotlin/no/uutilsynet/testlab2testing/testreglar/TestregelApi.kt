package no.uutilsynet.testlab2testing.testreglar

import no.uutilsynet.testlab2testing.dto.Regelsett
import no.uutilsynet.testlab2testing.dto.RegelsettDTO
import no.uutilsynet.testlab2testing.dto.TestregelDTO

interface TestregelApi {
  fun listTestreglar(): List<TestregelDTO>
  fun listRegelsett(): List<RegelsettDTO>
  fun createTestregel(testregelRequest: TestregelRequest): Int
  fun createRegelsett(regelsettRequest: RegelsettRequest): Int
  fun updateTestregel(testregel: TestregelDTO): TestregelDTO
  fun updateRegelsett(regelsett: Regelsett): Regelsett
  fun deleteTestregel(id: Int)
  fun deleteRegelsett(id: Int)
}
