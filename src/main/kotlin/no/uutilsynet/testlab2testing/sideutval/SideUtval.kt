package no.uutilsynet.testlab2testing.sideutval

import no.uutilsynet.testlab2testing.kontroll.SideutvalElementBase

open class SideUtval(val loeysingId: Int, val sideutvalElementBase: List<ISideutvalElement>) {

}

interface ISideutvalElement {
    val sti: SideutvalSti
}

class SideutvalElementAutomatisk(
    override val sti: SideutvalSti
) : ISideutvalElement