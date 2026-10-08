package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import io.micrometer.core.instrument.util.StringEscapeUtils

const val TITTEL = "{tittel}"
const val TEKST = "{tekst}"
const val AVSENDER = "{avsender}"

val epostTemplate = """
    <!DOCTYPE html>
    <html lang='no'>
        <head>
            <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />
            <title id='tittel'>$TITTEL</title>
        </head>
        <body style='font-family: sans-serif; padding: 40px 20px; color: #23262a; mso-line-height-rule: exactly;'>
            <h1 style='font-size: 32px; font-weight: bold'>Hei.</h1>
            <p>Vi har funnet nye kandidater for deg til stillingen: <b id='stillingstittel'>$TITTEL</b>.</p>
            <p style='white-space: pre-wrap; margin-top: 32px; margin-bottom: 32px' id='tekst'>$TEKST</p>
            <h3 style='font-size: 16px'>For å se kandidatene dine</h3>
            <div style='border: 16px solid #f2f3f5; border-radius: 12px; background-color: #f2f3f5;'>
                <p style='margin-top: 0'>For å se oversendte CV-er må du logge inn på &quot;Min side – arbeidsgiver&quot; på Nav sin nettside. Finn kandidater for dine stillinger der.  </p>
            </div>
            <p style='margin-top: 24px; margin-bottom: 40px;'>Vennlig hilsen <span id='avsender'>$AVSENDER</span></p>

            <div style='border: 1px solid #cbcfd5;'></div>

            <h2 style='font-size: 16px; margin-top: 40px; margin-bottom: 16px'>Mangler du tilgang til Min Side for Arbeidsgiver hos Nav?</h2>
            <div style='border: 24px solid #f2f3f5; border-radius: 12px; background-color: #f2f3f5'>
                <p style='margin-top: 0'>Tilgangen til Nav sine rekrutteringstjenester styrer arbeidsgivere selv i <b>Altinn</b>.</p>
                <p>For å få tilgang må du kontakte den som styrer tilgangene til virksomheten din. Det kan være noen i HR, en leder, mellomleder, eller noen på eiersiden i virksomheten.</p>
                <p>Vi har lagd en enkel forklaring du kan dele med vedkommende for å gjøre det enklere for hen å gi deg tilgang.</p>
                <p style='margin-bottom: 16px; border-bottom: 16px solid #f2f3f5;'>Kopier den gjerne og send den til vedkommende:</p>

                <div style='border: 3px dashed #cbcfd5; border-radius: 12px;'>
                    <div style='border: 24px solid #ffffff; border-radius: 8px; background-color: #ffffff;'>
                        <p style='margin-top: 0;'>Du får denne meldingen fordi avsender ønsker å få tilgang til CV-er fra Nav på vegne av virksomheten din.</p>
                        <p><b>Slik kan du delegere tilgang til oversendte CV-er fra Nav:</b></p>
                        <ul style='line-height: 24px'>
                            <li>Logg inn i Altinn</li>
                            <li>Velg virksomheten din og finn tilgangsstyring </li>
                            <li>Velg &quot;Enkelttjenester&quot;, og velg den enkelttjenesten som heter &quot;Rekrutteringssaker og CV-er fra Nav&quot; og oppgi den personen som skal få det tildelt</li>
                        </ul>
                        <p>Denne enkelttjenesten gir <i>kun</i> tilgang til å motta oversendte CV-er fra Nav på &quot;Min side – arbeidsgiver&quot; på Nav sitt nettsted. Arbeidsgiver kan derfor være trygg på at de ansatte som får denne enkelttjenesten ikke får tilgang til noe annet.</p>
                        <p><b>Ga ikke Altinn deg muligheten til å gi tilgang?</b></p>
                        <p>Du kan gi tilgang hvis du har en av disse rollene:</p>
                        <ul style='line-height: 24px'>
                            <li>Du er registrert i Enhetsregisteret som daglig leder, styrets leder, bestyrende reder eller innehaver.</li>
                            <li>Du har rollen &quot;Hovedadministrator&quot; i Altinn.</li>
                            <li>Du har tilgang som heter &quot;Tilgangsstyrer&quot; i Altinn, i tillegg til at du har den tilgangen du ønsker å delegere: enkelttjenesten &quot;Rekrutteringssaker og CV-er fra Nav&quot;. Dersom du både har &quot;Tilgangsstyrer&quot; og tilgangspakken &quot;Ansettelsesforhold&quot;, har du også autorisasjon til å delegere enkelttjenesten &quot;Rekrutteringssaker og CV-er fra Nav&quot;.</li>
                        </ul>
                    </div>
                </div>
            </div>
        </body>
    </html>
""".trimIndent()

fun lagEpostBody(tittel: String, tekst: String, avsender: String): String {
    return epostTemplate
        .replace(TITTEL, tittel.htmlEscape())
        .replace(TEKST, tekst.htmlEscape())
        .replace(AVSENDER, avsender)
        .oneLiner()
}

private fun String.htmlEscape(): String =
    replace("\n", "<br/>")
        .let { StringEscapeUtils.escapeJson(it) }
