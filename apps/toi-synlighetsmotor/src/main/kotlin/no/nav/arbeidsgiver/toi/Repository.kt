package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env.variable("NAIS_DATABASE_TOI_SYNIGHETSMOTOR_DB_HOST")
    private val port = env.variable("NAIS_DATABASE_TOI_SYNIGHETSMOTOR_DB_PORT")
    private val database = env.variable("NAIS_DATABASE_TOI_SYNIGHETSMOTOR_DB_DATABASE")
    private val user = env.variable("NAIS_DATABASE_TOI_SYNIGHETSMOTOR_DB_USERNAME")
    private val pw = env.variable("NAIS_DATABASE_TOI_SYNIGHETSMOTOR_DB_PASSWORD")

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

class Repository(val dataSource: DataSource) {

    private val tabell = "evaluering"
    private val aktøridKolonne = "aktor_id"

    private val fødselsnummerKolonne = "fodselsnummer"
    private val harAktivCvKolonne = "har_aktiv_cv"
    private val harJobbprofilkolonne = "har_jobbprofil"
    private val harSettHjemmelKolonne = "har_sett_hjemmel"
    private val måIkkeBehandleTidligereCvKolonne = "maa_ikke_behandle_tidligere_cv"
    private val ikkeFritattFraKandidatsøkKolonne = "ikke_fritatt_kandidatsok"
    private val erIkkeUnderOppfølgingKolonne = "er_under_oppfoelging"
    private val harRiktigFormidlingsgruppeKolonne = "har_riktig_formidlingsgruppe"
    private val erIkkeKode6Eller7Kolonne = "er_ikke_kode6_eller_kode7"
    private val erIkkeSperretAnsattKolonne = "er_ikke_sperret_ansatt"
    private val erIkkeDødKolonne = "er_ikke_doed"
    private val erFerdigBeregnetKolonne = "er_ferdig_beregnet"

    init {
        kjørFlywayMigreringer()
    }

    fun lagre(evaluering: Evaluering, aktørId: String, fødselsnummer: String?) {
        val databaseMap = evaluering.databaseMap(aktørId, fødselsnummer)
        val kolonneString = kolonneString(databaseMap.keys.toList())
        val verdiString = verdiString(databaseMap.values.toList())

        dataSource.connection.use {
            it.prepareStatement("insert into $tabell $kolonneString values $verdiString").apply {
                databaseMap.values.forEachIndexed { index, any ->
                    this.setObject(index + 1, any)
                }
            }.execute()
        }
    }

    fun hent(aktorId: String): Evaluering? =
        dataSource.connection.use {
            val resultset = it.prepareStatement("select * from $tabell where $aktøridKolonne = ?").apply {
                setString(1, aktorId)
            }.executeQuery()
            if (resultset.next()) {
                return Evaluering(
                    harAktivCv = resultset.getBoolean(harAktivCvKolonne),
                    harJobbprofil = resultset.getBoolean(harJobbprofilkolonne),
                    harSettHjemmel = resultset.getBoolean(harSettHjemmelKolonne),
                    maaIkkeBehandleTidligereCv = resultset.getBoolean(måIkkeBehandleTidligereCvKolonne),
                    erIkkefritattKandidatsøk = resultset.getBoolean(ikkeFritattFraKandidatsøkKolonne),
                    erUnderOppfoelging = resultset.getBoolean(erIkkeUnderOppfølgingKolonne),
                    harRiktigFormidlingsgruppe = resultset.getBoolean(harRiktigFormidlingsgruppeKolonne),
                    erIkkeKode6eller7 = resultset.getBoolean(erIkkeKode6Eller7Kolonne),
                    erIkkeSperretAnsatt = resultset.getBoolean(erIkkeSperretAnsattKolonne),
                    erIkkeDoed = resultset.getBoolean(erIkkeDødKolonne),
                    erFerdigBeregnet = resultset.getBoolean(erFerdigBeregnetKolonne),
                )

            } else null
        }


    private fun kolonneString(kolonner: List<String>) =
        kolonner.joinToString(prefix = "(", separator = ",", postfix = ")")

    private fun verdiString(verdier: List<Any?>) =
        verdier.map { "?" }.joinToString(prefix = "(", separator = ",", postfix = ")")

    private fun Evaluering.databaseMap(aktørId: String, fødselsnummer: String?): Map<String, Any?> {

        return mapOf(
            aktøridKolonne to aktørId,
            fødselsnummerKolonne to fødselsnummer,
            harAktivCvKolonne to harAktivCv,
            harJobbprofilkolonne to harJobbprofil,
            harSettHjemmelKolonne to harSettHjemmel,
            måIkkeBehandleTidligereCvKolonne to maaIkkeBehandleTidligereCv,
            ikkeFritattFraKandidatsøkKolonne to erIkkefritattKandidatsøk,
            erIkkeUnderOppfølgingKolonne to erUnderOppfoelging,
            harRiktigFormidlingsgruppeKolonne to harRiktigFormidlingsgruppe,
            erIkkeKode6Eller7Kolonne to erIkkeKode6eller7,
            erIkkeSperretAnsattKolonne to erIkkeSperretAnsatt,
            erIkkeDødKolonne to erIkkeDoed,
            erFerdigBeregnetKolonne to erFerdigBeregnet
        )
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }

}

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")