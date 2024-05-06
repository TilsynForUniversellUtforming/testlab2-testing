package no.uutilsynet.testlab2testing.testregel

open class InnhaldstypeTesting(open val id: Int, open val innhaldstype: String)

data class Tema(val id: Int, val tema: String)

data class Testobjekt(val id: Int, val testobjekt: String)
