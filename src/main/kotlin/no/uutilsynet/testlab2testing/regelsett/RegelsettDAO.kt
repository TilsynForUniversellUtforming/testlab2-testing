package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RegelsettDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun RegelsettBase.toRegelsett(): Regelsett {
    val testregelList =
        jdbcTemplate.query(
            """
        select tr.id, tr.testregel_id,tr.versjon,tr.namn, tr.krav_id, tr.status, tr.dato_sist_endra,tr.type , tr.modus ,tr.spraak,tr.tema,tr.testobjekt,tr.krav_til_samsvar,tr.testregel_schema,tr.innhaldstype_testing
        from regelsett_testregel rt
          join testregel tr on tr.id = rt.testregel_id
        where rt.regelsett_id = :regelsett_id
        order by tr.id
      """
                .trimIndent(),
            mapOf("regelsett_id" to this.id),
            testregelRowMapper)

    return Regelsett(
        this.id, this.namn, this.modus, this.standard, testregelList.map { it.toTestregelBase() })
  }

  fun toRegelsettResponse(regelsett: Regelsett): RegelsettResponse {
    return RegelsettResponse(
        regelsett.id, regelsett.namn, regelsett.modus, regelsett.standard, regelsett.testregelList)
  }

  fun getRegelsettResponse(int: Int): RegelsettResponse? {
    return getRegelsett(int)?.let { toRegelsettResponse(it) }
  }

  fun getRegelsett(id: Int): Regelsett? {
    val regelsettDTO =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                "select id, namn, modus, standard from regelsett where id = :id",
                mapOf("id" to id),
                DataClassRowMapper.newInstance(RegelsettBase::class.java)))

    return regelsettDTO?.toRegelsett()
  }

  @Cacheable("regelsettlistbase", unless = "#result.isEmpty()")
  fun getRegelsettBaseList(includeInactive: Boolean): List<RegelsettBase> {
    val activeSql = if (includeInactive) "1=1" else "aktiv = true"

    return jdbcTemplate.query(
        "select id, namn, modus, standard from regelsett where $activeSql",
        DataClassRowMapper.newInstance(RegelsettBase::class.java))
  }

  @Cacheable("regelsettlist", unless = "#result.isEmpty()")
  fun getRegelsettTestreglarList(includeInactive: Boolean): List<Regelsett> =
      getRegelsettBaseList(includeInactive).map { it.toRegelsett() }

  fun getRegelsettResponseList(includeInactive: Boolean): List<RegelsettResponse> =
      getRegelsettTestreglarList(includeInactive).map { toRegelsettResponse(it) }

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
}
