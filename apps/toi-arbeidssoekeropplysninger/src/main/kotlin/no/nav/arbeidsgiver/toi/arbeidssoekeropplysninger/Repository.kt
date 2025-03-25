package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.sql.DataSource

class Repository(private val datasource: DataSource) {
    companion object {
        private val logg = LoggerFactory.getLogger(Repository::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("secureLog")

        private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
    }

    fun lagreOppfølgingsperiodemelding(rapidOppfølgingsperiode: JsonNode): Long {
        sikkerLogg.info("Mottok rapid: $rapidOppfølgingsperiode")
        val periode = objectMapper.treeToValue<Periode>(rapidOppfølgingsperiode, Periode::class.java)
        // Ved konflikt/update så setter vi behandlet_dato=null for å sikre at ny komplett melding blir sendt på rapid
        datasource.connection.use { conn ->
            val sql = """
                insert into periodemelding(periode_id, identitetsnummer, aktor_id, periode_startet, periode_avsluttet, periode_mottatt_dato) 
                values(?, ?, ?, ?, ?, ?)
                on conflict(periode_id) do update
                set periode_startet = EXCLUDED.periode_startet,
                    identitetsnummer = EXCLUDED.identitetsnummer,
                    aktor_id = EXCLUDED.aktor_id,
                    periode_avsluttet = EXCLUDED.periode_avsluttet,
                    periode_mottatt_dato = EXCLUDED.periode_mottatt_dato,
                    behandlet_dato = null
                returning id
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setObject(1, periode.periodeId)
                setString(2, periode.identitetsnummer)
                setString(3, periode.aktørId)
                setTimestamp(4, Timestamp.from(periode.startet.toInstant()))
                if (periode.avsluttet == null)
                    setNull(5, Types.TIMESTAMP)
                else
                    setTimestamp(5, Timestamp.from(periode.avsluttet.toInstant()))
                setTimestamp(6, Timestamp.from(Instant.now()))
            }.use { statement ->
                val rs = statement.executeQuery()
                if (rs.next()) {
                    val id = rs.getLong(1)
                    return id
                }
            }
        }
        return 0
    }

    fun lagreArbeidssøkeropplysninger(arbeidssokerOpplysninger: List<OpplysningerOmArbeidssoeker>): List<Long> {
        val ider = mutableListOf<Long>()
        // NB: Det hadde vært en bedre løsning å bruke TxTemplate fra pam-cv-api-gcp
        datasource.connection.use { conn ->
            val autoCommit = conn.autoCommit
            try {
                conn.autoCommit = false
                arbeidssokerOpplysninger.forEach {
                    ider.add(lagreArbeidssøkeropplysninger(it, conn))
                }
                conn.commit()
            } catch (t: Throwable) {
                conn.rollback()
                throw t
            } finally {
                conn.autoCommit = autoCommit
            }
        }

        return ider
    }

    fun lagreArbeidssøkeropplysninger(arbeidssokerOpplysninger: OpplysningerOmArbeidssoeker): Long {
        datasource.connection.use { conn ->
            return lagreArbeidssøkeropplysninger(arbeidssokerOpplysninger, conn)
        }
    }

    private fun lagreArbeidssøkeropplysninger(arbeidssokerOpplysninger: OpplysningerOmArbeidssoeker, conn: Connection): Long {
        sikkerLogg.info("Mottok rapid: $arbeidssokerOpplysninger")

        // Ved konflikt/update så setter vi behandlet_dato=null for å sikre at ny komplett melding blir sendt på rapid
        val sql = """
            insert into periodemelding(periode_id, helsetilstand_hindrer_arbeid, andre_forhold_hindrer_arbeid, opplysninger_mottatt_dato) 
            values(?, ?, ?, ?)
            on conflict(periode_id) do update
            set helsetilstand_hindrer_arbeid = EXCLUDED.helsetilstand_hindrer_arbeid,
                andre_forhold_hindrer_arbeid = EXCLUDED.andre_forhold_hindrer_arbeid,
                opplysninger_mottatt_dato = EXCLUDED.opplysninger_mottatt_dato,
                behandlet_dato = null
            returning id
        """.trimIndent()
        conn.prepareStatement(sql).apply {
            setObject(1, arbeidssokerOpplysninger.periodeId)
            setBoolean(2, arbeidssokerOpplysninger.helse?.helsetilstandHindrerArbeid == JaNeiVetIkke.JA)
            setBoolean(3, arbeidssokerOpplysninger.annet?.andreForholdHindrerArbeid == JaNeiVetIkke.JA)
            setTimestamp(4, Timestamp.from(Instant.now()))
        }.use { statement ->
            val rs = statement.executeQuery()
            if (rs.next()) {
                val id = rs.getLong(1)
                return id
            }
        }
        return 0
    }

    fun hentPeriodeOpplysninger(periodeId: UUID): PeriodeOpplysninger? {
        datasource.connection.use { conn ->
            val sql = """
                select id, periode_id, identitetsnummer, aktor_id, periode_startet, periode_avsluttet, periode_mottatt_dato,
                         opplysninger_mottatt_dato,
                         behandlet_dato,
                         helsetilstand_hindrer_arbeid,
                         andre_forhold_hindrer_arbeid
                from periodemelding
                where periode_id=?
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setObject(1, periodeId)
            }.use { statement ->
                val rs = statement.executeQuery()
                if (rs.next()) {
                    return PeriodeOpplysninger.fraDatabase(rs)
                } else {
                    return null
                }
            }
        }
    }

    fun behandlePeriodeOpplysning(periodeId: UUID): Boolean {
        datasource.connection.use { conn ->
            val sql = """
                update periodemelding
                set behandlet_dato = ?
                where periode_id= ?
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setTimestamp(1, Timestamp.from(Instant.now()))
                setObject(2, periodeId)
            }.use { statement ->
                val rader = statement.executeUpdate()
                return rader > 0
            }
        }

    }

    /**
     * Henter ubehandlede periodeopplysniger - kun hvis vi har mottatt periodemelding med identitetsnummer og aktørId
     */
    fun hentUbehandledePeriodeOpplysninger(limit: Int = 1000): List<PeriodeOpplysninger> {
        val periodeOpplysninger = mutableListOf<PeriodeOpplysninger>()
        datasource.connection.use { conn ->
            val sql = """
                select id, periode_id, identitetsnummer, aktor_id, periode_startet, periode_avsluttet, periode_mottatt_dato,
                         opplysninger_mottatt_dato,
                         behandlet_dato,
                         helsetilstand_hindrer_arbeid,
                         andre_forhold_hindrer_arbeid
                from periodemelding
                where 
                  behandlet_dato is null and opplysninger_mottatt_dato is not null and identitetsnummer is not null
                  and aktor_id is not null
                order by periode_mottatt_dato asc
                limit ?
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setInt(1, limit)
            }.use { statement ->
                val rs = statement.executeQuery()
                while (rs.next()) {
                    periodeOpplysninger.add(PeriodeOpplysninger.fraDatabase(rs))
                }
            }
        }
        return periodeOpplysninger
    }
}


data class Periode(
    @JsonProperty("periode_id")
    val periodeId: UUID,
    val identitetsnummer: String,
    val aktørId: String?,
    val startet: ZonedDateTime,
    val avsluttet: ZonedDateTime?
)

data class PeriodeOpplysninger(
    val id: Long? = null,
    @JsonProperty("periode_id")
    val periodeId: UUID,
    val identitetsnummer: String? = null,
    val aktørId: String? = null,
    @JsonProperty("periode_startet")
    val periodeStartet: ZonedDateTime? = null,
    @JsonProperty("periode_avsluttet")
    val periodeAvsluttet: ZonedDateTime? = null,
    @JsonProperty("periode_mottatt")
    val periodeMottattDato: ZonedDateTime? = null,
    @JsonProperty("opplysninger_mottatt")
    val opplysningerMottattDato: ZonedDateTime? = null,
    @JsonProperty("behandlet_dato")
    val behandletDato: ZonedDateTime? = null, // Tidspunkt for når komplett melding er publisert på rapid
    @JsonProperty("helsetilstand_hindrer_arbeid")
    val helsetilstandHindrerArbeid: Boolean? = null,
    @JsonProperty("andre_forhold_hindrer_arbeid")
    val andreForholdHindrerArbeid: Boolean? = null
) {
    companion object {
        fun fraDatabase(rs: ResultSet) = PeriodeOpplysninger(
            id = rs.getLong("id"),
            periodeId = rs.getObject("periode_id", UUID::class.java),
            identitetsnummer = rs.getString("identitetsnummer"),
            aktørId = rs.getString("aktor_id"),
            periodeStartet = rs.getTimestamp("periode_startet")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
            periodeAvsluttet = rs.getTimestamp("periode_avsluttet")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
            periodeMottattDato = rs.getTimestamp("periode_mottatt_dato")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
            opplysningerMottattDato = rs.getTimestamp("opplysninger_mottatt_dato")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
            behandletDato = rs.getTimestamp("behandlet_dato")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
            helsetilstandHindrerArbeid = rs.getBoolean("helsetilstand_hindrer_arbeid").let { if (rs.wasNull()) null else it },
            andreForholdHindrerArbeid = rs.getBoolean("andre_forhold_hindrer_arbeid").let { if (rs.wasNull()) null else it }
        )
    }
}