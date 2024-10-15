package no.uutilsynet.testlab2testing.styringsdata

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Klagetype
import no.uutilsynet.testlab2.constants.Reaksjonstype
import no.uutilsynet.testlab2.constants.ResultatKlage
import no.uutilsynet.testlab2.constants.StyringsdataKontrollStatus
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Kontroll.*
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.Bot
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.Klage
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.Paalegg
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StyringsdataDAO(private val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun Timestamp.toLocalDate(): LocalDate = this.toInstant().atZone(ZONEID_OSLO).toLocalDate()

  fun Timestamp?.toLocalDateOrNull() = this?.toInstant()?.atZone(ZONEID_OSLO)?.toLocalDate()

  fun LocalDate?.toTimestampNullable() = this?.atStartOfDay()?.let { Timestamp.valueOf(it) }

  private val styringsdataListElementRowMapper = { rs: ResultSet, _: Int ->
    StyringsdataListElement(
        id = rs.getInt("id"),
        kontrollId = rs.getInt("kontroll_id"),
        loeysingId = rs.getInt("loeysing_id"),
        ansvarleg = rs.getString("ansvarleg"),
        oppretta = rs.getTimestamp("oppretta").toLocalDate(),
        frist = rs.getTimestamp("frist").toLocalDate(),
        reaksjon = Reaksjonstype.valueOf(rs.getString("reaksjon")),
        paaleggReaksjon = Reaksjonstype.valueOf(rs.getString("paalegg_reaksjon")),
        paaleggKlageReaksjon = Reaksjonstype.valueOf(rs.getString("paalegg_klage_reaksjon")),
        botReaksjon = Reaksjonstype.valueOf(rs.getString("bot_reaksjon")),
        botKlageReaksjon = Reaksjonstype.valueOf(rs.getString("bot_klage_reaksjon")),
        paaleggId = rs.getInt("paalegg_id").takeUnless { rs.wasNull() },
        paaleggKlageId = rs.getInt("paalegg_klage_id").takeUnless { rs.wasNull() },
        botId = rs.getInt("bot_id").takeUnless { rs.wasNull() },
        botKlageId = rs.getInt("bot_klage_id").takeUnless { rs.wasNull() },
        sistLagra = rs.getTimestamp("sist_lagra").toInstant())
  }

  private val styringsdataKontrollRowMapper = { rs: ResultSet, _: Int ->
    Styringsdata.Kontroll(
        id = rs.getInt("id"),
        kontrollId = rs.getInt("kontroll_id"),
        ansvarleg = rs.getString("ansvarleg"),
        oppretta = rs.getTimestamp("oppretta").toLocalDateOrNull(),
        frist = rs.getTimestamp("frist").toLocalDateOrNull(),
        varselSendtDato = rs.getTimestamp("varsel_sendt_dato").toLocalDateOrNull(),
        status =
            rs.getString("status")
                .takeUnless { rs.wasNull() }
                ?.let { StyringsdataKontrollStatus.valueOf(it) },
        foerebelsRapportSendtDato =
            rs.getTimestamp("foerebels_rapport_sendt_dato").toLocalDateOrNull(),
        svarFoerebelsRapportDato =
            rs.getTimestamp("svar_foerebels_rapport_dato").toLocalDateOrNull(),
        endeligRapportDato = rs.getTimestamp("endelig_rapport_dato").toLocalDateOrNull(),
        kontrollAvsluttaDato = rs.getTimestamp("kontroll_avslutta_dato").toLocalDateOrNull(),
        rapportPublisertDato = rs.getTimestamp("rapport_publisert_dato").toLocalDateOrNull(),
        sistLagra = rs.getTimestamp("sist_lagra")?.toInstant(),
    )
  }

  fun getStyringsdataLoeysing(id: Int): List<StyringsdataListElement> =
      jdbcTemplate.query(
          """
      select
        id,
        kontroll_id,
        loeysing_id,
        ansvarleg,
        oppretta,
        frist,
        reaksjon,
        paalegg_reaksjon,
        paalegg_klage_reaksjon,
        bot_reaksjon,
        bot_klage_reaksjon,
        paalegg_id,
        paalegg_klage_id,
        bot_id,
        bot_klage_id,
        sist_lagra
      from styringsdata_loeysing
        where id = :id 
    """
              .trimIndent(),
          mapOf("id" to id),
          styringsdataListElementRowMapper)

  fun getStyringsdataKontroll(
      styringsdataId: Int,
  ): List<Styringsdata.Kontroll> =
      jdbcTemplate.query(
          """
      select
        id,
        kontroll_id,
        ansvarleg,
        oppretta,
        frist,
        varsel_sendt_dato,
        status,
        foerebels_rapport_sendt_dato,
        svar_foerebels_rapport_dato,
        endelig_rapport_dato,
        kontroll_avslutta_dato,
        rapport_publisert_dato,
        sist_lagra
      from styringsdata_kontroll
        where id = :id 
    """
              .trimIndent(),
          mapOf("id" to styringsdataId),
          styringsdataKontrollRowMapper)

  fun findStyringsdataKontroll(
      kontrollId: Int,
  ): Styringsdata.Kontroll? =
      jdbcTemplate
          .query(
              """
      select
        id,
        kontroll_id,
        ansvarleg,
        oppretta,
        frist,
        varsel_sendt_dato,
        status,
        foerebels_rapport_sendt_dato,
        svar_foerebels_rapport_dato,
        endelig_rapport_dato,
        kontroll_avslutta_dato,
        rapport_publisert_dato,
        sist_lagra
      from styringsdata_kontroll
        where kontroll_id = :kontroll_id 
    """
                  .trimIndent(),
              mapOf("kontroll_id" to kontrollId),
              styringsdataKontrollRowMapper)
          .firstOrNull()

  fun findStyringsdataLoeysing(
      kontrollId: Int,
  ): List<StyringsdataListElement> =
      jdbcTemplate.query(
          """
      select
        id,
        kontroll_id,
        loeysing_id,
        ansvarleg,
        oppretta,
        frist,
        reaksjon,
        paalegg_reaksjon,
        paalegg_klage_reaksjon,
        bot_reaksjon,
        bot_klage_reaksjon,
        paalegg_id,
        paalegg_klage_id,
        bot_id,
        bot_klage_id,
        sist_lagra
      from styringsdata_loeysing
        where kontroll_id = :kontrollId 
    """
              .trimIndent(),
          mapOf("kontrollId" to kontrollId),
          styringsdataListElementRowMapper)

  fun getPaalegg(id: Int): Paalegg? =
      jdbcTemplate.queryForObject(
          "select id, vedtak_dato, frist from styringsdata_loeysing_paalegg where id = :id",
          mapOf("id" to id),
          DataClassRowMapper.newInstance(Paalegg::class.java))

  fun getBot(id: Int): Bot? =
      jdbcTemplate.queryForObject(
          """
            select
                id, beloep_dag, oeking_etter_dager, oekning_type, oeking_sats, 
                vedtak_dato, start_dato, slutt_dato, kommentar
            from styringsdata_loeysing_bot 
              where id = :id"""
              .trimIndent(),
          mapOf("id" to id),
      ) { rs, _ ->
        Bot(
            id = rs.getInt("id"),
            beloepDag = rs.getInt("beloep_dag"),
            oekingEtterDager = rs.getInt("oeking_etter_dager"),
            oekningType = BotOekningType.valueOf(rs.getString("oekning_type")),
            oekingSats = rs.getInt("oeking_sats"),
            vedtakDato = rs.getTimestamp("vedtak_dato").toLocalDate(),
            startDato = rs.getTimestamp("start_dato").toLocalDate(),
            sluttDato = rs.getTimestamp("slutt_dato").toLocalDateOrNull(),
            kommentar = rs.getString("kommentar"))
      }

  fun getKlage(id: Int): Klage? =
      jdbcTemplate.queryForObject(
          """
            select
                id, klage_mottatt_dato, klage_avgjort_dato, resultat_klage_tilsyn, 
                klage_dato_departement, resultat_klage_departement
            from styringsdata_loeysing_klage 
              where id = :id"""
              .trimIndent(),
          mapOf("id" to id),
      ) { rs, _ ->
        Klage(
            id = rs.getInt("id"),
            klageMottattDato = rs.getTimestamp("klage_mottatt_dato").toLocalDate(),
            klageAvgjortDato = rs.getTimestamp("klage_avgjort_dato").toLocalDateOrNull(),
            resultatKlageTilsyn =
                rs.getString("resultat_klage_tilsyn")?.let { ResultatKlage.valueOf(it) },
            klageDatoDepartement =
                rs.getTimestamp("klage_dato_departement")
                    ?.toInstant()
                    ?.atZone(ZONEID_OSLO)
                    ?.toLocalDate(),
            resultatKlageDepartement =
                rs.getString("resultat_klage_departement")?.let { ResultatKlage.valueOf(it) })
      }

  @Transactional
  fun createStyringsdataLoeysing(styringsdata: Styringsdata.Loeysing): Result<Int> = runCatching {
    val paaleggId = styringsdata.paalegg?.let { insertPaalegg(it) }
    val paaleggKlageId = styringsdata.paaleggKlage?.let { insertKlage(it, Klagetype.paalegg) }
    val botId = styringsdata.bot?.let { insertBot(it) }
    val botKlageId = styringsdata.botKlage?.let { insertKlage(it, Klagetype.bot) }
    val sistLagra = Timestamp.from(Instant.now())

    jdbcTemplate.queryForObject(
        """
        insert into styringsdata_loeysing (
          ansvarleg, 
          oppretta, 
          frist, 
          reaksjon, 
          paalegg_reaksjon,
          paalegg_klage_reaksjon,
          bot_reaksjon,
          bot_klage_reaksjon,
          paalegg_id,
          paalegg_klage_id,
          bot_id,
          bot_klage_id,
          kontroll_id,
          loeysing_id,
          sist_lagra
        ) values (
          :ansvarleg, 
          :oppretta,
          :frist,
          :reaksjon,
          :paalegg_reaksjon,
          :paalegg_klage_reaksjon,
          :bot_reaksjon,
          :bot_klage_reaksjon,
          :paalegg_id,
          :paalegg_klage_id,
          :bot_id,
          :bot_klage_id,
          :kontroll_id,
          :loeysing_id,
          :sist_lagra
        ) returning id
      """
            .trimIndent(),
        mapOf(
            "ansvarleg" to styringsdata.ansvarleg,
            "oppretta" to Timestamp.valueOf(styringsdata.oppretta.atStartOfDay()),
            "frist" to Timestamp.valueOf(styringsdata.frist.atStartOfDay()),
            "reaksjon" to styringsdata.reaksjon.name,
            "paalegg_reaksjon" to styringsdata.paaleggReaksjon.name,
            "paalegg_klage_reaksjon" to styringsdata.paaleggKlageReaksjon.name,
            "bot_reaksjon" to styringsdata.botReaksjon.name,
            "bot_klage_reaksjon" to styringsdata.botKlageReaksjon.name,
            "paalegg_id" to paaleggId,
            "paalegg_klage_id" to paaleggKlageId,
            "bot_id" to botId,
            "bot_klage_id" to botKlageId,
            "kontroll_id" to styringsdata.kontrollId,
            "loeysing_id" to styringsdata.loeysingId,
            "sist_lagra" to sistLagra),
        Int::class.java)!!
  }

  @Transactional
  fun updateStyringsdataLoeysing(id: Int, styringsdata: Styringsdata.Loeysing) {
    val paaleggId =
        styringsdata.paalegg?.let { if (it.id != null) updatePaalegg(it) else insertPaalegg(it) }
    val paaleggKlageId =
        styringsdata.paaleggKlage?.let {
          if (it.id != null) updateKlage(it) else insertKlage(it, Klagetype.paalegg)
        }
    val botId = styringsdata.bot?.let { if (it.id != null) updateBot(it) else insertBot(it) }
    val botKlageId =
        styringsdata.botKlage?.let {
          if (it.id != null) updateKlage(it) else insertKlage(it, Klagetype.bot)
        }
    val sistLagra = Timestamp.from(Instant.now())

    jdbcTemplate.update(
        """
              update styringsdata_loeysing set
                  ansvarleg = :ansvarleg,
                  oppretta = :oppretta,
                  frist = :frist,
                  reaksjon = :reaksjon,
                  paalegg_reaksjon = :paalegg_reaksjon,
                  paalegg_klage_reaksjon = :paalegg_klage_reaksjon,
                  bot_reaksjon = :bot_reaksjon,
                  bot_klage_reaksjon = :bot_klage_reaksjon,
                  paalegg_id = :paalegg_id,
                  paalegg_klage_id = :paalegg_klage_id,
                  bot_id = :bot_id,
                  bot_klage_id = :bot_klage_id,
                  sist_lagra = :sist_lagra
              where id = :id
          """
            .trimIndent(),
        mapOf(
            "id" to id,
            "ansvarleg" to styringsdata.ansvarleg,
            "oppretta" to Timestamp.valueOf(styringsdata.oppretta.atStartOfDay()),
            "frist" to Timestamp.valueOf(styringsdata.frist.atStartOfDay()),
            "reaksjon" to styringsdata.reaksjon.name,
            "paalegg_reaksjon" to styringsdata.paaleggReaksjon.name,
            "paalegg_klage_reaksjon" to styringsdata.paaleggKlageReaksjon.name,
            "bot_reaksjon" to styringsdata.botReaksjon.name,
            "bot_klage_reaksjon" to styringsdata.botKlageReaksjon.name,
            "paalegg_id" to paaleggId,
            "paalegg_klage_id" to paaleggKlageId,
            "bot_id" to botId,
            "bot_klage_id" to botKlageId,
            "sist_lagra" to sistLagra))
  }

  @Transactional
  fun createStyringsdataKontroll(styringsdata: Styringsdata.Kontroll): Result<Int> = runCatching {
    val sistLagra = Timestamp.from(Instant.now())

    jdbcTemplate.queryForObject(
        """
        insert into styringsdata_kontroll (
          kontroll_id,
          ansvarleg,
          oppretta,
          frist,
          varsel_sendt_dato,
          status,
          foerebels_rapport_sendt_dato,
          svar_foerebels_rapport_dato,
          endelig_rapport_dato,
          kontroll_avslutta_dato,
          rapport_publisert_dato,
          sist_lagra
        ) values (
          :kontroll_id,
          :ansvarleg,
          :oppretta,
          :frist,
          :varsel_sendt_dato,
          :status,
          :foerebels_rapport_sendt_dato,
          :svar_foerebels_rapport_dato,
          :endelig_rapport_dato,
          :kontroll_avslutta_dato,
          :rapport_publisert_dato,
          :sist_lagra
        ) returning id
      """
            .trimIndent(),
        mapOf(
            "kontroll_id" to styringsdata.kontrollId,
            "ansvarleg" to styringsdata.ansvarleg,
            "oppretta" to styringsdata.oppretta.toTimestampNullable(),
            "frist" to styringsdata.frist.toTimestampNullable(),
            "varsel_sendt_dato" to styringsdata.varselSendtDato.toTimestampNullable(),
            "status" to styringsdata.status?.name,
            "foerebels_rapport_sendt_dato" to
                styringsdata.foerebelsRapportSendtDato.toTimestampNullable(),
            "svar_foerebels_rapport_dato" to
                styringsdata.svarFoerebelsRapportDato.toTimestampNullable(),
            "endelig_rapport_dato" to styringsdata.endeligRapportDato.toTimestampNullable(),
            "kontroll_avslutta_dato" to styringsdata.kontrollAvsluttaDato.toTimestampNullable(),
            "rapport_publisert_dato" to styringsdata.rapportPublisertDato.toTimestampNullable(),
            "sist_lagra" to sistLagra,
        ),
        Int::class.java)!!
  }

  @Transactional
  fun updateStyringsdataKontroll(id: Int, styringsdata: Styringsdata.Kontroll) {
    val sistLagra = Timestamp.from(Instant.now())

    jdbcTemplate.update(
        """
              update styringsdata_kontroll set
ansvarleg = :ansvarleg,
oppretta = :oppretta,
frist = :frist,
varsel_sendt_dato = :varsel_sendt_dato,
status = :status,
foerebels_rapport_sendt_dato = :foerebels_rapport_sendt_dato,
svar_foerebels_rapport_dato = :svar_foerebels_rapport_dato,
endelig_rapport_dato = :endelig_rapport_dato,
kontroll_avslutta_dato = :kontroll_avslutta_dato,
rapport_publisert_dato = :rapport_publisert_dato,
sist_lagra = :sist_lagra
              where id = :id
          """
            .trimIndent(),
        mapOf(
            "id" to id,
            "ansvarleg" to styringsdata.ansvarleg,
            "oppretta" to styringsdata.oppretta.toTimestampNullable(),
            "frist" to styringsdata.frist.toTimestampNullable(),
            "varsel_sendt_dato" to styringsdata.varselSendtDato.toTimestampNullable(),
            "status" to styringsdata.status?.name,
            "foerebels_rapport_sendt_dato" to
                styringsdata.foerebelsRapportSendtDato.toTimestampNullable(),
            "svar_foerebels_rapport_dato" to
                styringsdata.svarFoerebelsRapportDato.toTimestampNullable(),
            "endelig_rapport_dato" to styringsdata.endeligRapportDato.toTimestampNullable(),
            "kontroll_avslutta_dato" to styringsdata.kontrollAvsluttaDato.toTimestampNullable(),
            "rapport_publisert_dato" to styringsdata.rapportPublisertDato.toTimestampNullable(),
            "sist_lagra" to sistLagra))
  }

  private fun insertPaalegg(paalegg: Paalegg): Int {
    val id =
        jdbcTemplate.queryForObject(
            "insert into styringsdata_loeysing_paalegg (vedtak_dato, frist) values (:vedtak_dato, :frist) returning id",
            mapOf(
                "vedtak_dato" to Timestamp.valueOf(paalegg.vedtakDato.atStartOfDay()),
                "frist" to paalegg.frist.toTimestampNullable()),
            Int::class.java)!!

    return id
  }

  private fun updatePaalegg(paalegg: Paalegg): Int {
    jdbcTemplate.update(
        "update styringsdata_loeysing_paalegg set vedtak_dato = :vedtak_dato, frist = :frist where id = :id",
        mapOf(
            "id" to paalegg.id,
            "vedtak_dato" to Timestamp.valueOf(paalegg.vedtakDato.atStartOfDay()),
            "frist" to paalegg.frist.toTimestampNullable()))

    return paalegg.id!!
  }

  private fun insertKlage(klage: Klage, klagetype: Klagetype): Int {
    val id =
        jdbcTemplate.queryForObject(
            """
        insert into styringsdata_loeysing_klage (
          klage_type, klage_mottatt_dato, klage_avgjort_dato, resultat_klage_tilsyn, klage_dato_departement, resultat_klage_departement
        ) values (
          :klage_type, :klage_mottatt_dato, :klage_avgjort_dato, :resultat_klage_tilsyn, :klage_dato_departement, :resultat_klage_departement
        ) returning id
      """
                .trimIndent(),
            mapOf(
                "klage_type" to klagetype.name,
                "klage_mottatt_dato" to Timestamp.valueOf(klage.klageMottattDato.atStartOfDay()),
                "klage_avgjort_dato" to klage.klageAvgjortDato.toTimestampNullable(),
                "resultat_klage_tilsyn" to klage.resultatKlageTilsyn?.name,
                "klage_dato_departement" to klage.klageDatoDepartement.toTimestampNullable(),
                "resultat_klage_departement" to klage.resultatKlageDepartement?.name),
            Int::class.java)!!

    return id
  }

  private fun updateKlage(klage: Klage): Int {
    val sql =
        """
            update styringsdata_loeysing_klage set
                klage_mottatt_dato = :klage_mottatt_dato,
                klage_avgjort_dato = :klage_avgjort_dato,
                resultat_klage_tilsyn = :resultat_klage_tilsyn,
                klage_dato_departement = :klage_dato_departement,
                resultat_klage_departement = :resultat_klage_departement
            where id = :id
          """

    val params =
        mapOf(
            "id" to klage.id,
            "klage_mottatt_dato" to Timestamp.valueOf(klage.klageMottattDato.atStartOfDay()),
            "klage_avgjort_dato" to klage.klageAvgjortDato.toTimestampNullable(),
            "resultat_klage_tilsyn" to klage.resultatKlageTilsyn?.name,
            "klage_dato_departement" to klage.klageDatoDepartement.toTimestampNullable(),
            "resultat_klage_departement" to klage.resultatKlageDepartement?.name)

    jdbcTemplate.update(sql, params)

    return klage.id!!
  }

  private fun insertBot(bot: Bot): Int {
    val id =
        jdbcTemplate.queryForObject(
            """ 
              insert into styringsdata_loeysing_bot (
                beloep_dag, oeking_etter_dager, oekning_type, oeking_sats, vedtak_dato, start_dato, slutt_dato, kommentar
              ) values (
                :beloep_dag, :oeking_etter_dager, :oekning_type, :oeking_sats, :vedtak_dato, :start_dato, :slutt_dato, :kommentar
              )            
              returning id
          """
                .trimIndent(),
            mapOf(
                "beloep_dag" to bot.beloepDag,
                "oeking_etter_dager" to bot.oekingEtterDager,
                "oekning_type" to bot.oekningType.name,
                "oeking_sats" to bot.oekingSats,
                "vedtak_dato" to Timestamp.valueOf(bot.vedtakDato.atStartOfDay()),
                "start_dato" to Timestamp.valueOf(bot.startDato.atStartOfDay()),
                "slutt_dato" to bot.sluttDato.toTimestampNullable(),
                "kommentar" to bot.kommentar),
            Int::class.java)!!

    return id
  }

  private fun updateBot(bot: Bot): Int {
    jdbcTemplate.update(
        """
        update styringsdata_loeysing_bot set
          beloep_dag = :beloep_dag,
          oeking_etter_dager = :oeking_etter_dager,
          oekning_type = :oekning_type,
          oeking_sats = :oeking_sats,
          vedtak_dato = :vedtak_dato,
          start_dato = :start_dato,
          slutt_dato = :slutt_dato,
          kommentar = :kommentar
        where id = :id
        """
            .trimIndent(),
        mapOf(
            "id" to bot.id,
            "beloep_dag" to bot.beloepDag,
            "oeking_etter_dager" to bot.oekingEtterDager,
            "oekning_type" to bot.oekningType.name,
            "oeking_sats" to bot.oekingSats,
            "vedtak_dato" to Timestamp.valueOf(bot.vedtakDato.atStartOfDay()),
            "start_dato" to Timestamp.valueOf(bot.startDato.atStartOfDay()),
            "slutt_dato" to bot.sluttDato.toTimestampNullable(),
            "kommentar" to bot.kommentar))

    return bot.id!!
  }
}
