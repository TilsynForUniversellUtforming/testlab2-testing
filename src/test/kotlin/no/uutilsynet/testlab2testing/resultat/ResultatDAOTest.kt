package no.uutilsynet.testlab2testing.resultat

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResultatDAOTest(@Autowired val resultatDAO: ResultatDAO) {

  @Test
  fun setWhereClause() {
    val kontrollId = 1
    val kontrolltype = Kontrolltype.ForenklaKontroll
    val startDato = LocalDate.of(2024, 1, 1)
    val sluttDato = LocalDate.of(2024, 12, 31)

    val whereClause = resultatDAO.setWhereClause(kontrollId, kontrolltype, startDato, sluttDato)
    assertEquals(
        "where k.id = 1 and kontrolltype = 'ForenklaKontroll' and ag.dato >= '2024-01-01' and ag.dato <= '2024-12-31'",
        whereClause)
  }

  @Test
  fun emptyWhereClause() {
    val whereClause = resultatDAO.setWhereClause(null, null, null, null)
    assertEquals("", whereClause)
  }

  @Test
  fun emptyKontrollIdSetWhereClause() {
    val kontrolltype = Kontrolltype.ForenklaKontroll
    val startDato = LocalDate.of(2024, 1, 1)
    val sluttDato = LocalDate.of(2024, 12, 31)

    val whereClause = resultatDAO.setWhereClause(null, kontrolltype, startDato, sluttDato)
    assertEquals(
        "where kontrolltype = 'ForenklaKontroll' and ag.dato >= '2024-01-01' and ag.dato <= '2024-12-31'",
        whereClause)
  }

  @Test
  fun setWhereClauseKontrollType() {
    val kontrolltype = Kontrolltype.ForenklaKontroll

    val whenClause = resultatDAO.setWhereClause(null, kontrolltype, null, null)
    assertEquals("where kontrolltype = 'ForenklaKontroll'", whenClause)
  }
}
