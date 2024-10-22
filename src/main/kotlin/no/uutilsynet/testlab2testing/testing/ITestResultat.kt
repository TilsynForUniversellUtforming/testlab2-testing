package no.uutilsynet.testlab2testing.testing

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar
import java.time.Instant

interface ITestResultat {
    val loeysingId:Int;
    val testregelId:Int;
    val sideutvalId:Int;
    val elementOmtale: String?;
    val elemenResultat: TestresultatUtfall?;
    val elementUtfall:String?;
    val brukar:Brukar?;
    val testVartUtfoert: Instant?;
}