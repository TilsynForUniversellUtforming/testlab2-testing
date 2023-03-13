package no.uutilsynet.testlab2testing.common

import java.sql.Array as SqlArray
import java.sql.SQLDataException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

object JdbcUtil {
  fun <T> NamedParameterJdbcTemplate.getPgArrayVarchar(list: List<T>): SqlArray {
    val typedList = list.map { it.toString() }.toTypedArray()
    return jdbcTemplate.dataSource?.connection?.createArrayOf("varchar", typedList)
        ?: throw SQLDataException()
  }

  fun NamedParameterJdbcTemplate.getPgArrayNumber(list: List<Int>): SqlArray {
    val typedList = list.toTypedArray()
    return jdbcTemplate.dataSource?.connection?.createArrayOf("number", typedList)
        ?: throw SQLDataException()
  }

  @Suppress("UNCHECKED_CAST") fun <T> SqlArray.asList(): List<T> = (this.array as Array<T>).toList()
}
