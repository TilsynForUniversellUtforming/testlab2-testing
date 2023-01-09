package no.uutilsynet.testlab2testing.dao

import no.uutilsynet.testlab2testing.dto.Test
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.JdbcTemplate

class TestingDao(val jdbcTemplate: JdbcTemplate) {

  private final val listTestingSql = "select * from test"

  fun listTesting(): List<Test> =
    jdbcTemplate.query(listTestingSql, DataClassRowMapper.newInstance(Test::class.java))
}
