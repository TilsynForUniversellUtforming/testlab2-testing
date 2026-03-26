package no.uutilsynet.testlab2testing.resultat.common

import java.sql.ResultSet
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestgrunnlagType
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregelDTO
import java.util.UUID

fun resultatLoeysingRowmapper(rs: ResultSet): ResultatPerTestregelDTO {
  val maalingId = rs.getInt("id")
  val navn = rs.getString("tittel")
  val dato = handleDate(rs.getDate("dato"))
  val kontrolltype = Kontrolltype.valueOf(rs.getString("kontrolltype"))
  val loeysingId = rs.getInt("loeysing_id")
  val testregelGjennomsnittlegSideSamsvarProsent =
      rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent")
  val talElementSamsvar = rs.getInt("tal_element_samsvar")
  val talElementBrot = rs.getInt("tal_element_brot")
  val testregelId = rs.getInt("testregel_id")
  val testgrunnlagId = rs.getInt("testgrunnlag_id")
  val testtype = setTestType(kontrolltype, rs)
  val testrunUuid = rs.getString("testrun_uuid")

  return ResultatPerTestregelDTO(
      maalingId,
      testgrunnlagId,
      navn,
      kontrolltype,
      TestgrunnlagType.valueOf(testtype),
      dato,
      listOf("testar"),
      loeysingId,
      testregelGjennomsnittlegSideSamsvarProsent,
      talElementSamsvar,
      talElementBrot,
      testregelId,
      UUID.fromString(testrunUuid))
}

fun handleDate(date: java.sql.Date?): LocalDate {
  if (date != null) {
    return date.toLocalDate()
  }
  return LocalDate.now()
}

fun setTestType(kontrolltype: Kontrolltype, resultSet: ResultSet): String {
  if (kontrolltype == Kontrolltype.ForenklaKontroll) {
    return TestgrunnlagType.OPPRINNELEG_TEST.toString()
  }
  return resultSet.getString("testtype")
}
