package no.uutilsynet.testlab2testing.regelsett

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegelsettDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun getRegelsett(id: Int): RegelsettBase? {
    val regelsettDTO =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                "select id, namn, modus, standard from regelsett where id = :id",
                mapOf("id" to id),
                DataClassRowMapper.newInstance(RegelsettBase::class.java)))

    return regelsettDTO
  }

  @Cacheable("regelsettlistbase", unless = "#result.isEmpty()")
  fun getRegelsettBaseList(includeInactive: Boolean): List<RegelsettBase> {
    val activeSql = if (includeInactive) "1=1" else "aktiv = true"

    return jdbcTemplate.query(
        "select id, namn, modus, standard from regelsett where $activeSql",
        DataClassRowMapper.newInstance(RegelsettBase::class.java))
  }

  @Transactional
  @CacheEvict(cacheNames = ["regelsett", "regelsettlist", "regelsettlistbase"], allEntries = true)
  fun createRegelsett(regelsett: RegelsettCreate): Int {
    val id =
        jdbcTemplate.queryForObject(
            "insert into regelsett (namn, modus, standard, aktiv) values (:namn, :modus, :standard, true) returning id",
            mapOf(
                "namn" to regelsett.namn,
                "modus" to regelsett.modus.value,
                "standard" to regelsett.standard,
            ),
            Int::class.java)!!

    val updateBatchValuesRegelsettTestregel =
        regelsett.testregelIdList.map { mapOf("regelsett_id" to id, "testregel_id" to it) }

    jdbcTemplate.batchUpdate(
        "insert into regelsett_testregel (regelsett_id, testregel_id) values (:regelsett_id, :testregel_id)",
        updateBatchValuesRegelsettTestregel.toTypedArray())

    return id
  }

  @Transactional
  @CacheEvict(
      key = "#regelsett.id", cacheNames = ["regelsett", "regelsettlist", "regelsettlistbase"])
  fun updateRegelsett(regelsett: RegelsettEdit) {
    jdbcTemplate.update(
        "delete from regelsett_testregel where regelsett_id = :regelsett_id ",
        mapOf("regelsett_id" to regelsett.id))

    jdbcTemplate.update(
        """
      update regelsett set
      namn = :namn,
      standard = :standard
      where id = :id
      """
            .trimIndent(),
        mapOf("namn" to regelsett.namn, "standard" to regelsett.standard, "id" to regelsett.id))

    val updateBatchValuesRegelsettTestregel =
        regelsett.testregelIdList.map {
          mapOf("regelsett_id" to regelsett.id, "testregel_id" to it)
        }

    jdbcTemplate.batchUpdate(
        "insert into regelsett_testregel (regelsett_id, testregel_id) values (:regelsett_id, :testregel_id)",
        updateBatchValuesRegelsettTestregel.toTypedArray())
  }

  @Transactional
  @CacheEvict(cacheNames = ["regelsett", "regelsettlist", "regelsettlistbase"], allEntries = true)
  fun deleteRegelsett(id: Int) =
      jdbcTemplate.update("update regelsett set aktiv = false where id = :id", mapOf("id" to id))

  fun getTestregelIdListForRegelsett(id: Int): List<Int> {
    return jdbcTemplate.queryForList(
        "select testregel_id from regelsett_testregel where regelsett_id = :id",
        mapOf("id" to id),
        Int::class.java)
  }
}
