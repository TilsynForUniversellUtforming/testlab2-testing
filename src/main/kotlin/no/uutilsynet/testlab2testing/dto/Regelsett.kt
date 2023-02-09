package no.uutilsynet.testlab2testing.dto

data class Regelsett(override val id: Int, override val namn: String, val testregelList: List<Testregel>): RegelsettDTO()
