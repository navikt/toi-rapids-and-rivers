package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import org.flywaydb.core.Flyway
import java.sql.ResultSet
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_HOST")
    private val port = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PORT")
    private val database = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_DATABASE")
    private val user = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_USERNAME")
    private val pw = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PASSWORD")


    fun lagDatasource() = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$database"
        minimumIdle = 1
        maximumPoolSize = 2
        driverClassName = "org.postgresql.Driver"
        initializationFailTimeout = 5000
        username = user
        password = pw
        validate()
    }.let(::HikariDataSource)
}

class Repository(private val dataSource: DataSource) {

    private val aktørIdKolonne = "aktor_id"
    private val kandidatKolonne = "kandidat"
    private val sammenstiltkandidatTabell = "sammenstiltkandidat"

    init {
        kjørFlywayMigreringer()
    }

    fun lagreKandidat(kandidat: Kandidat) {
        val kandidatFinnes = hentKandidat(kandidat.aktørId) != null

        dataSource.connection.use {
            if (kandidatFinnes) {
                it.prepareStatement("UPDATE $sammenstiltkandidatTabell SET $kandidatKolonne = ? WHERE $aktørIdKolonne = ?")
                    .apply {
                        setString(1, kandidat.toJson())
                        setString(2, kandidat.aktørId)
                    }
            } else {
                it.prepareStatement("insert into $sammenstiltkandidatTabell($aktørIdKolonne, $kandidatKolonne) VALUES (?,?)")
                    .apply {
                        setString(1, kandidat.aktørId)
                        setString(2, kandidat.toJson())
                    }
            }.executeUpdate()
        }
    }

    fun hentKandidat(aktørId: String) = dataSource.connection.use {
        val statement =
            it.prepareStatement("select $kandidatKolonne from $sammenstiltkandidatTabell where $aktørIdKolonne = ?")
        statement.setString(1, aktørId)
        val resultSet = statement.executeQuery()
        if (resultSet.next())
            Kandidat.fraJson(resultSet.getString(1))
        else null
    }

    fun gjørOperasjonPåAlleKandidaterIndexed(operasjon: (Kandidat, Int) -> Unit) {
        val connection = dataSource.connection

        connection.autoCommit = false

        connection.use {
            val statement = it.prepareStatement("select $kandidatKolonne from $sammenstiltkandidatTabell", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).also { stmt ->
                stmt.fetchSize = 10
            }
            val resultSet = statement.executeQuery()

            resultSet.forEachRowIndexed { resultSetRow, index ->
                val kandidat = Kandidat.fraJson(resultSetRow.getString(1))
                operasjon(kandidat, index)
            }
        }
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

data class Kandidat(
    val aktørId: String,
    val arbeidsmarkedCv: JsonNode? = null,
    val veileder: JsonNode? = null,
    val oppfølgingsinformasjon: JsonNode? = null,
    val siste14avedtak: JsonNode? = null,
    val oppfølgingsperiode: JsonNode? = null,
    val arenaFritattKandidatsøk: JsonNode? = null,
    val hjemmel: JsonNode? = null,
    val måBehandleTidligereCv: JsonNode? = null,
    val kvp: JsonNode? = null,
    val arbeidssøkerperiode : JsonNode? = null,
    val arbeidssøkeropplysninger : JsonNode? = null
) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
        fun fraJson(json: String): Kandidat = fraJson(objectMapper.readTree(json))
        fun fraJson(json: JsonNode) = Kandidat(
            aktørId = json["aktørId"].asText(),
            arbeidsmarkedCv = json["arbeidsmarkedCv"],
            veileder = json["veileder"],
            oppfølgingsinformasjon = json["oppfølgingsinformasjon"],
            siste14avedtak = json["siste14avedtak"],
            oppfølgingsperiode = json["oppfølgingsperiode"],
            arenaFritattKandidatsøk = json["arenaFritattKandidatsøk"],
            hjemmel = json["hjemmel"],
            måBehandleTidligereCv = json["måBehandleTidligereCv"],
            kvp = json["kvp"],
            arbeidssøkerperiode = json["arbeidssøkerperiode"],
            arbeidssøkeropplysninger = json["arbeidssøkeropplysninger"],
        )
    }

    private fun somJsonUtenNullFelt(): String {
        val objectMapper = jacksonObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
        return objectMapper.writeValueAsString(this)
    }

    @Deprecated(message = "Se på å bruk populerMelding for å istedetfor gjenbruke eksisterende melding")
    fun somJsonMessage(meterRegistry: MeterRegistry) = JsonMessage(somJsonUtenNullFelt(), MessageProblems(""), meterRegistry)

    fun toJson() = jacksonObjectMapper().writeValueAsString(this)
    fun populerMelding(packet: JsonMessage): JsonMessage {
        val jsonObjekt = jacksonObjectMapper().valueToTree<JsonNode>(this)
        jsonObjekt.fieldNames().forEach { feltNavn ->
            packet[feltNavn] = jsonObjekt.get(feltNavn)
        }
        return packet
    }
}

typealias AktøridHendelse = Pair<String, JsonMessage>

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")

private fun ResultSet.forEachRowIndexed(operation: (ResultSet, Int) -> Unit) {
    var teller = 0

    while (this.next()) {
        operation(this, teller++)
    }
}


