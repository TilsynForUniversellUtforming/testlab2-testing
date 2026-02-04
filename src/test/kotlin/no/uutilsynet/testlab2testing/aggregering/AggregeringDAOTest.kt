package no.uutilsynet.testlab2testing.aggregering

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.resultat.OpprettTestgrunnlag
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.datasource.url= jdbc:tc:postgresql:16-alpine:///test-db"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregeringDAOTest(
    @Autowired val aggregeringDAO: AggregeringDAO,
    @Autowired val testUtils: TestUtils
) {

  fun setUp(): Int {
    val testregelId = testUtils.createTestregelKrav().id
    val testgrunnlag = OpprettTestgrunnlag("Testgrunnlag", TestgrunnlagType.OPPRINNELEG_TEST)
    val kontroll =
        testUtils.createKontroll(
            "Kontroll", Kontrolltype.InngaaendeKontroll, listOf(1), testregelId)

      println("Kontroll oppretta med id: ${kontroll}")

    return testUtils.createTestgrunnlag(testgrunnlag, kontroll)
  }

  @Test
  fun nullVerdiarBlirIkkjeKonverterFraDB() {
    val testgrunnlagId = setUp()

    val testresultat =
        AggregeringPerTestregelDB(
            maalingId = null,
            loeysingId = 1,
            testregelId = 1,
            testgrunnlagId = testgrunnlagId,
            suksesskriterium = 1,
            fleireSuksesskriterium = listOf(1, 2, 3),
            talElementBrot = 0,
            talElementSamsvar = 0,
            talElementIkkjeForekomst = 0,
            talElementVarsel = 0,
            talSiderBrot = 0,
            talSiderSamsvar = 0,
            talSiderIkkjeForekomst = 0,
            testregelGjennomsnittlegSideBrotProsent = null,
            testregelGjennomsnittlegSideSamsvarProsent = null,
        )

    val updated = aggregeringDAO.createAggregertResultatTestregel(testresultat)
    assert(updated > 0)

    val henta = aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)

    Assertions.assertThat(henta[0].testregelGjennomsnittlegSideBrotProsent).isNull()
    Assertions.assertThat(henta[0].testregelGjennomsnittlegSideSamsvarProsent).isNull()

    aggregeringDAO.deleteAggregertResultatTestregel(henta[0])
  }
}
