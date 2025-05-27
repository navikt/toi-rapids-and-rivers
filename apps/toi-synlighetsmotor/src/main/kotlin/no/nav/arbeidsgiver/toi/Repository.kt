package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.ResultSet
import javax.sql.DataSource

class Repository(private val dataSource: DataSource) {
    private val tabell = "evaluering"
    private val aktøridKolonne = "aktor_id"
    private val fødselsnummerKolonne = "fodselsnummer"

    private val harAktivCvKolonne = "har_aktiv_cv"
    private val harJobbprofilkolonne = "har_jobbprofil"
    private val harSettHjemmelKolonne = "har_sett_hjemmel"
    private val måIkkeBehandleTidligereCvKolonne = "maa_ikke_behandle_tidligere_cv"
    private val arenaIkkeFritattKandidatsøkKolonne = "arena_ikke_fritatt_kandidatsok"
    private val erIkkeUnderOppfølgingKolonne = "er_under_oppfolging"
    private val harRiktigFormidlingsgruppeKolonne = "har_riktig_formidlingsgruppe"
    private val erIkkeKode6Eller7Kolonne = "er_ikke_kode6_eller_kode7"
    private val erIkkeSperretAnsattKolonne = "er_ikke_sperret_ansatt"
    private val erIkkeDødKolonne = "er_ikke_doed"
    private val erIkkeKvpKolonne = "er_ikke_kvp"
    private val erArbeidssøkerKolonne = "er_arbeidssoker"
    private val erFerdigBeregnetKolonne = "er_ferdig_beregnet"

    fun lagre(evaluering: Evaluering, aktørId: String, fødselsnummer: String?) {
        val databaseMap = evaluering.databaseMap(aktørId, fødselsnummer)
        val kolonneString = kolonneString(databaseMap.keys.toList())
        val verdiString = verdiString(databaseMap.values.toList())
        val updateString = updateString(databaseMap)

        val evaluering = hentMedAktørid(aktørId)
        if (evaluering != null) {
            dataSource.connection.use {
                it.prepareStatement(
                    "update $tabell set $updateString where $aktøridKolonne = '$aktørId'"
                ).apply {
                    databaseMap.filterKeys { it != aktøridKolonne }.values
                        .forEachIndexed { index, any ->
                            this.setObject((index + 1), any)
                        }
                }.execute()
            }
        } else {
            dataSource.connection.use {
                it.prepareStatement("insert into $tabell $kolonneString values $verdiString").apply {
                    databaseMap.values.forEachIndexed { index, any ->
                        this.setObject(index + 1, any)
                    }
                }.execute()
            }
        }
    }

    private fun updateString(data: Map<String, Any?>): String {
        return data.entries
            .filter { it.key != aktøridKolonne }
            .joinToString(transform = { "${it.key} = ?" }, separator = ",")
    }

    fun hentMedAktørid(aktorId: String): Evaluering? =
        dataSource.connection.use {
            val resultset = it.prepareStatement("select * from $tabell where $aktøridKolonne = ?").apply {
                setString(1, aktorId)
            }.executeQuery()
            if (resultset.next()) {
                return evalueringFraDB(resultset)
            } else null
        }

    fun hentMedFnr(aktorId: String): Evaluering? =
        dataSource.connection.use {
            val resultset = it.prepareStatement("select * from $tabell where $fødselsnummerKolonne = ?").apply {
                setString(1, aktorId)
            }.executeQuery()
            if (resultset.next()) {
                return evalueringFraDB(resultset)
            } else null
        }

    fun hentEvalueringer(fødselsnummerliste: List<String>): Map<String, Evaluering> {
        dataSource.connection.use {
            val resultset = it.prepareStatement("select * from $tabell where $fødselsnummerKolonne = ANY(?)").apply {
                setArray(1, dataSource.connection.createArrayOf("varchar", fødselsnummerliste.toTypedArray()))
            }.executeQuery()

            return sequence {
                while (resultset.next()) {
                    resultset.getString(fødselsnummerKolonne).let { fnr ->
                        if (fnr != null) {
                            yield(fnr to evalueringFraDB(resultset))
                        }
                    }
                }
            }.toMap()
        }
    }


    private fun evalueringFraDB(resultset: ResultSet) = Evaluering(
        harAktivCv = resultset.getBoolean(harAktivCvKolonne).tilBooleanVerdi(), // TODO
        harJobbprofil = resultset.getBoolean(harJobbprofilkolonne).tilBooleanVerdi(), // TODO
        harSettHjemmel = resultset.getBoolean(harSettHjemmelKolonne).tilBooleanVerdi(), // TODO
        maaIkkeBehandleTidligereCv = resultset.getBoolean(måIkkeBehandleTidligereCvKolonne).tilBooleanVerdi(), // TODO
        arenaIkkeFritattKandidatsøk = resultset.getBoolean(arenaIkkeFritattKandidatsøkKolonne).tilBooleanVerdi(), // TODO
        erUnderOppfoelging = resultset.getBoolean(erIkkeUnderOppfølgingKolonne).tilBooleanVerdi(), // TODO
        harRiktigFormidlingsgruppe = resultset.getBoolean(harRiktigFormidlingsgruppeKolonne).tilBooleanVerdi(), // TODO
        erIkkeKode6eller7 = resultset.getBoolean(erIkkeKode6Eller7Kolonne).tilBooleanVerdi(), // TODO
        erIkkeSperretAnsatt = resultset.getBoolean(erIkkeSperretAnsattKolonne).tilBooleanVerdi(), // TODO
        erIkkeDoed = resultset.getBoolean(erIkkeDødKolonne).tilBooleanVerdi(), // TODO
        erIkkeKvp = resultset.getBoolean(erIkkeKvpKolonne).tilBooleanVerdi(), // TODO
        harIkkeAdressebeskyttelse = BooleanVerdi.missing, // TODO denne har vi ikke i databasen ennå
        erArbeidssøker = resultset.getBoolean(erArbeidssøkerKolonne).tilBooleanVerdi(),
        komplettBeregningsgrunnlag = resultset.getBoolean(erFerdigBeregnetKolonne)
    )

    private fun kolonneString(kolonner: List<String>) =
        kolonner.joinToString(prefix = "(", separator = ",", postfix = ")")

    private fun verdiString(verdier: List<Any?>) =
        verdier.map { "?" }.joinToString(prefix = "(", separator = ",", postfix = ")")

    private fun Evaluering.databaseMap(aktørId: String, fødselsnummer: String?): Map<String, Any?> {
        return mapOf(
            aktøridKolonne to aktørId,
            fødselsnummerKolonne to fødselsnummer,
            harAktivCvKolonne to harAktivCv.default(true),     //TODO
            harJobbprofilkolonne to harJobbprofil.default(true),     //TODO
            harSettHjemmelKolonne to harSettHjemmel.default(true),     //TODO
            måIkkeBehandleTidligereCvKolonne to maaIkkeBehandleTidligereCv.default(true),     //TODO
            arenaIkkeFritattKandidatsøkKolonne to (arenaIkkeFritattKandidatsøk.default(true)
                    && harIkkeAdressebeskyttelse.default(true)),     //TODO
            erIkkeUnderOppfølgingKolonne to erUnderOppfoelging.default(true),     //TODO
            harRiktigFormidlingsgruppeKolonne to harRiktigFormidlingsgruppe.default(true),     //TODO
            erIkkeKode6Eller7Kolonne to erIkkeKode6eller7.default(true),     //TODO
            erIkkeSperretAnsattKolonne to erIkkeSperretAnsatt.default(true),     //TODO
            erIkkeDødKolonne to erIkkeDoed.default(true),     //TODO
            erIkkeKvpKolonne to erIkkeKvp.default(true),     //TODO
            erArbeidssøkerKolonne to erArbeidssøker.default(false), //???
            erFerdigBeregnetKolonne to erFerdigBeregnet
        )
    }

    fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env.variable("NAIS_DATABASE_TOI_SYNLIGHETSMOTOR_SYNLIGHETSMOTOR_DB_HOST")
    private val port = env.variable("NAIS_DATABASE_TOI_SYNLIGHETSMOTOR_SYNLIGHETSMOTOR_DB_PORT")
    private val database = env.variable("NAIS_DATABASE_TOI_SYNLIGHETSMOTOR_SYNLIGHETSMOTOR_DB_DATABASE")
    private val user = env.variable("NAIS_DATABASE_TOI_SYNLIGHETSMOTOR_SYNLIGHETSMOTOR_DB_USERNAME")
    private val pw = env.variable("NAIS_DATABASE_TOI_SYNLIGHETSMOTOR_SYNLIGHETSMOTOR_DB_PASSWORD")

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

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")
