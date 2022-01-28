package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * Programmet krever at du har startet cloud_sql_proxy og at du har satt
 * databasebruker og databasepassord i 'Program arguments' slik:
 *
 * <bruker> <passord>
 */
fun main(args: Array<String>) {
    val databasebruker = args.getOrNull(0)?.toString() ?: throw RuntimeException("Databasebruker ikke satt")
    val databasepassord = args.getOrNull(1)?.toString() ?: throw RuntimeException("Databasepassord ikke satt")

    val tilkobling = databasetilkobling(databasebruker, databasepassord)
    println("Har koblet til databasen")

    gjørOperasjonPåAlleKandidaterIndexed(tilkobling) { kandidat, i ->
        println("Hentet ut kandidat nr. $i")
    }
}

fun gjørOperasjonPåAlleKandidaterIndexed(connection: Connection, operasjon: (Kandidat, Int) -> Unit) {
    connection.autoCommit = false

    connection.use { it ->
        val statement = it.prepareStatement("select kandidat from sammenstiltkandidat", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).also { stmt ->
            stmt.fetchSize = 1
        }
        val resultSet = statement.executeQuery()

        resultSet.forEachRowIndexed { resultSetRow, index ->
            val kandidat = Kandidat.fraJson(resultSetRow.getString(1))
            operasjon(kandidat, index)
        }
    }
}

private fun ResultSet.forEachRowIndexed(operation: (ResultSet, Int) -> Unit) {
    var teller = 0

    while (this.next()) {
        operation(this, teller++)
    }
}


fun hentKandidatRader(databaseTilkobling: Connection, sql: String) =
    databaseTilkobling.prepareStatement(sql)
        .executeQuery()
        .map { databaseRad ->
            databaseRad.getString("kandidat")
        }

val databasetilkobling = { bruker: String, passord: String ->
    DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/db",
        bruker,
        passord
    )
}

fun <T> ResultSet.map(mapper: (ResultSet) -> T): List<T> {
    return generateSequence {
        if (this.next()) {
            mapper(this)
        } else {
            null
        }
    }.toList()
}

fun String.somJsonNode() = jacksonObjectMapper().readTree(this)
fun JsonNode.har(felt: String) = this[felt] != null && !this[felt].isNull
