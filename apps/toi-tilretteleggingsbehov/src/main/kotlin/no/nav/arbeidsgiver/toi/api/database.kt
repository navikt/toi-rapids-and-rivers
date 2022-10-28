package no.nav.arbeidsgiver.toi.api

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

private val tilretteleggingsbehovTabell = "tilretteleggingsbehov"
private val fødselsnummerKolonne = "fødselsnummer"
private val sistEndretAvNavIdentKolonne = "sistEndretAvNavIdent"
private val sistEndretTidspunktKolonne = "sistEndretTidspunkt"
private val arbeidstidKolonne = "arbeidstid"
private val fysiskKolonne = "fysisk"
private val arbeidshverdagenKolonne = "arbeidshverdagen"
private val utfordringerMedNorskKolonne = "utfordringerMedNorsk"

// NB: Kun brukes for rest-endepunkt
// TODO: Generaliser
fun lagre(tilretteleggingsbehov: TilretteleggingsbehovInput, dataSource: DataSource): Tilretteleggingsbehov {
    val eksisterendeTilretteleggingsbehov = hentTilretteleggingsbehov(tilretteleggingsbehov.fnr, dataSource)
    val skalOppdatere = eksisterendeTilretteleggingsbehov?.sistEndretTidspunkt?.isBefore(LocalDateTime.now()) ?: false

    val statement = if (skalOppdatere) {
        dataSource.connection.prepareStatement(
            """
                update $tilretteleggingsbehovTabell
                set 
                    $sistEndretAvNavIdentKolonne = ?,
                    $sistEndretTidspunktKolonne = ?,
                    $arbeidshverdagenKolonne = ?,
                    $fysiskKolonne = ?,
                    $arbeidshverdagenKolonne = ?,
                    $utfordringerMedNorskKolonne = ?
                where $fødselsnummerKolonne = ${tilretteleggingsbehov.fnr}
            """.trimIndent()
        ).apply {
            setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
            setString(2, "G-DUMMY")
            // TODO: Lag noe felles for lesing og insert for triksing av lister
            setString(3, tilretteleggingsbehov.arbeidshverdagen.joinToString(", "))
            setString(4, tilretteleggingsbehov.fysisk.joinToString(", "))
            setString(5, tilretteleggingsbehov.arbeidshverdagen.joinToString(", "))
            setString(6, tilretteleggingsbehov.utfordringerMedNorsk.joinToString(", "))
        }

    } else {
        dataSource.connection.prepareStatement(
            """
               insert into $tilretteleggingsbehovTabell(
                   $fødselsnummerKolonne, 
                   $sistEndretTidspunktKolonne, 
                   $sistEndretAvNavIdentKolonne, 
                   $arbeidstidKolonne, 
                   $fysiskKolonne, 
                   $arbeidshverdagenKolonne, 
                   $utfordringerMedNorskKolonne)
               values (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).apply {
            setString(1, tilretteleggingsbehov.fnr)
            setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
            setString(3, "G-DUMMY")
            // TODO: Lag noe felles for lesing og insert for triksing av lister
            setString(4, tilretteleggingsbehov.arbeidstid.joinToString(", "))
            setString(5, tilretteleggingsbehov.fysisk.joinToString(", "))
            setString(6, tilretteleggingsbehov.arbeidshverdagen.joinToString(", "))
            setString(7, tilretteleggingsbehov.utfordringerMedNorsk.joinToString(", "))
        }
    }

    val suksess = statement.executeUpdate() == 1

    if (suksess) {
        return hentTilretteleggingsbehov(tilretteleggingsbehov.fnr, dataSource)!!
    } else {
        throw RuntimeException("Kunne ikke lagre tilretteleggingsbehov")
    }
}

// NB: Brukes for lagring fra topic (pga tidpsunkt må brukes og ikke bare settes nytt)
private fun lagre(tilretteleggingsbehov: Tilretteleggingsbehov, dataSource: DataSource) {

}


fun lagreFraTopic() {

}

fun hentTilretteleggingsbehov(fødselsnummer: Fødselsnummer, dataSource: DataSource): Tilretteleggingsbehov? {
    val resultSet =
        dataSource.connection.prepareStatement("select * from $tilretteleggingsbehovTabell where $fødselsnummerKolonne = ?")
            .apply {
                setString(1, fødselsnummer)
            }.executeQuery()

    return if (resultSet.next()) {
        resultSet.tilTilretteleggingsbehov()
    } else {
        null
    }
}

fun republiserAlleRader() {
    TODO("Se på hvordan sammenstilleren gjør det")
}

private fun ResultSet.tilTilretteleggingsbehov() =
    Tilretteleggingsbehov(
        fnr = getString(fødselsnummerKolonne),
        sistEndretAv = getString(sistEndretAvNavIdentKolonne),
        sistEndretTidspunkt = getTimestamp(sistEndretTidspunktKolonne).toLocalDateTime(),
        arbeidstid = getString(arbeidstidKolonne).split(", ").filter{it != ""}.map { Arbeidstid.valueOf(it) }.toSet(),
        fysisk = getString(fysiskKolonne).split(", ").filter{it != ""}.map { Fysisk.valueOf(it) }.toSet(),
        arbeidshverdagen = getString(arbeidshverdagenKolonne).split(", ").filter{it != ""}.map { Arbeidshverdagen.valueOf(it) }.toSet(),
        utfordringerMedNorsk = getString(utfordringerMedNorskKolonne).split(", ").filter{it != ""}
            .map { UtfordringerMedNorsk.valueOf(it) }.toSet(),
    )
