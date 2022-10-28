package no.nav.arbeidsgiver.toi.api

import java.sql.ResultSet
import javax.sql.DataSource

private val tilretteleggingsbehovTabell = "tilretteleggingsbehov"
private val fødselsnummerKolonne = "fødselsnummerKolonne"
private val arbeidstidKolonne = "arbeidstid"
private val fysiskKolonne = "fysisk"
private val arbeidshverdagenKolonne = "arbeidshverdagen"
private val utfordringerMedNorskKolonne = "utfordringerMedNorsk"
private val sistEndretAvNavIdentKolonne = "sistEndretAvNavIdent"
private val sistEndretTidspunktKolonne = "sistEndretTidspunkt"

fun lagre(tilretteleggingsbehov: TilretteleggingsbehovInput, dataSource: DataSource): Tilretteleggingsbehov {
    TODO("")
}

fun hentTilretteleggingsbehov(fødselsnummer: String, dataSource: DataSource): Tilretteleggingsbehov? {
    val resultSet = dataSource.connection.prepareStatement("select * from $tilretteleggingsbehovTabell where $fødselsnummerKolonne = ?").apply {
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
        fødselsnummer = getString(fødselsnummerKolonne),
        sistEndretAvNavIdent = getString(sistEndretAvNavIdentKolonne),
        sistEndretTidspunkt = getTimestamp(sistEndretTidspunktKolonne).toLocalDateTime(),
        arbeidstid = getString(arbeidstidKolonne).split(", ").map { Arbeidstid.valueOf(it) }.toSet(),
        fysisk = getString(fysiskKolonne).split(", ").map { Fysisk.valueOf(it) }.toSet(),
        arbeidshverdagen = getString(arbeidshverdagenKolonne).split(", ").map { Arbeidshverdagen.valueOf(it) }.toSet(),
        utfordringerMedNorsk = getString(utfordringerMedNorskKolonne).split(", ").map { UtfordringerMedNorsk.valueOf(it) }.toSet(),
    )
