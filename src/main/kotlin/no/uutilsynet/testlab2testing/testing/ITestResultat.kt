package no.uutilsynet.testlab2testing.testing

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar

interface ITestResultat {
  val loeysingId: Int

  val testregelId: Int

  val sideutvalId: Int

  val elementOmtale: String?

  val elemenResultat: TestresultatUtfall?

  val elementUtfall: String?

  val brukar: Brukar?

  val testVartUtfoert: Instant?
}
