package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregeringDAOTest(@Autowired val aggregeringDAO: AggregeringDAO) {

  @Test
  fun nullVerdiarBlirIkkjeKonverterFraDB() {
    val testresultat =
        AggregeringPerTestregelDTO(
            maalingId = null,
            loeysingId = 1,
            testregelId = 1,
            testgrunnlagId = 1,
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

    val henta = aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(1)

    println(henta[0])

    assertThat(henta[0].testregelGjennomsnittlegSideBrotProsent).isNull()
    assertThat(henta[0].testregelGjennomsnittlegSideSamsvarProsent).isNull()

    aggregeringDAO.deleteAggregertResultatTestregel(henta[0])
  }
}
