package no.uutilsynet.testlab2testing.sideutval

import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.loeysing.Loeysing

sealed class SideUtval(
    open val loeysing: Loeysing,
    open val sideutvalElement: List<ISideutvalElement>
) {}

class SideUtvalAutomatisk(
    override val loeysing: Loeysing,
    override val sideutvalElement: List<SideutvalElementAutomatisk>
) : SideUtval(loeysing, sideutvalElement) {
  fun getNettsider(): List<URL> = sideutvalElement.map { URI(it.sti.sti()).toURL() }
}

interface ISideutvalElement {
  val sti: SideutvalSti
}

class SideutvalElementAutomatisk(override val sti: SideutvalSti) : ISideutvalElement
