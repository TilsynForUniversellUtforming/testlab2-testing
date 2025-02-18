package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.resultat.OpprettTestgrunnlag
import org.assertj.core.api.Assertions.assertThat
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
    val testregelId = testUtils.createTestregel()
    val testgrunnlag = OpprettTestgrunnlag("Testgrunnlag", TestgrunnlagType.OPPRINNELEG_TEST)
    val kontroll =
        testUtils.createKontroll(
            "Kontroll", Kontrolltype.InngaaendeKontroll, listOf(1), testregelId)

    return testUtils.createTestgrunnlag(testgrunnlag, kontroll)
  }

  @Test
  fun nullVerdiarBlirIkkjeKonverterFraDB() {
    val testgrunnlagId = setUp()

    val testresultat =
        AggregeringPerTestregelDTO(
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

    assertThat(henta[0].testregelGjennomsnittlegSideBrotProsent).isNull()
    assertThat(henta[0].testregelGjennomsnittlegSideSamsvarProsent).isNull()

    aggregeringDAO.deleteAggregertResultatTestregel(henta[0])
  }
}
