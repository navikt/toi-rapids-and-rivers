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

    // NB: Tar lang tid å lese alle rader uten begrensninger
    val sql = "select kandidat from sammenstiltkandidat"
    val kandidatRader = hentKandidatRader(tilkobling, sql)
    println("Har hentet ut rader fra databasen")

    val kandidaterSomJsonNoder = kandidatRader.map { it.somJsonNode() }

    // Bruk
    val antall = kandidaterSomJsonNoder.filter {
        it.har("måBehandleTidligereCv")
    }.map {
        it["fritattKandidatsøk"]["fritattKandidatsok"].asBoolean()
    }.filter { it }.size

    println("Antall kandidater med måBehandleTidligereCv true: ${antall}")
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
