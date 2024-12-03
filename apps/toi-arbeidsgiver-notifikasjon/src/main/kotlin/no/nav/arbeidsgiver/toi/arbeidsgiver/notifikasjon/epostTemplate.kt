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
                <ol style='margin: 0px; padding-left: 24px; line-height: 24px; background-color: #f2f3f5;'>
                    <li>På Nav sitt nettsted, under arkfanen &quot;Arbeidsgiver&quot;, velg &quot;Min side - arbeidsgiver&quot;.</li>
                    <li><b>Logg inn</b></li>
                    <li>Finn <b>varslene dine</b> oppe til høyre på siden, og trykk deg inn på meldingen, eller finn lenken i kortet med teksten <b>Kandidater til mine stillinger</b> lenger ned på siden.</li>
                </ol>
            </div>
            <p style='margin-top: 24px; margin-bottom: 40px;'>Vennlig hilsen <span id='avsender'>$AVSENDER</span></p>

            <div style='border: 1px solid #cbcfd5;'></div>

            <h2 style='font-size: 16px; margin-top: 40px; margin-bottom: 16px'>Mangler du tilgang til Min Side for Arbeidsgiver hos Nav?</h2>
            <div style='border: 24px solid #f2f3f5; border-radius: 12px; background-color: #f2f3f5'>
                <p style='margin-top: 0'>Tilgangen til NAVs rekrutteringstjenester styrer arbeidsgivere selv i <b>Altinn</b>.</p>
                <p>For å få tilgang må du kontakte den som styrer tilgangene til virksomheten din. Det kan være noen i HR, en leder, mellomleder, eller noen på eiersiden i virksomheten.</p>
                <p>Vi har lagd en oppskrift du kan dele med vedkommende for å gjøre det enklere for dem å gi deg tilgang.</p>
                <p style='margin-bottom: 0; border-bottom: 16px solid #f2f3f5;'>Kopier den gjerne og send den til vedkommende:</p>

                <div style='border: 3px dashed #cbcfd5; border-radius: 12px;'>
                    <div style='border: 24px solid #ffffff; border-radius: 8px; background-color: #ffffff;'>
                        <p style='margin-top: 0;'>Du får denne meldingen fordi avsender ønsker å få tilgang til CV-er fra Nav på vegne av virksomheten din.</p>
                        <p><b>Gi tilganger til CV-er fra Nav:</b></p>
                        <ol style='line-height: 24px'>
                            <li>Logg inn i Altinn</li>
                            <li>Velg virksomheten din under «Alle dine aktører»</li>
                            <li>Trykk på knappen «Profil» øverst i menyen</li>
                            <li>Trykk på «Andre med rettigheter til virksomheten»</li>
                            <li>Velg «Legge til ny person eller virksomhet»</li>
                            <li>Legg inn personnummeret og etternavnet til personen som skal ha tilgang</li>
                            <li>Velg «Gi tilgang til enkelttjenester»</li>
                            <li>Skriv «Rekruttering», så vil alternativet komme opp som et valg. Velg «Rekruttering».</li>
                            <li>Bekreft</li>
                        </ol>
                        <p>Denne enkeltrettigheten gir kun tilgang til å bruke Nav sine rekrutteringstjenester: publisere stillingsannonser og holde videomøter for stillinger på Arbeidsplassen, og motta CV-er fra Nav på &quot;Min side - arbeidsgiver&quot; på Nav sitt nettsted.</p>
                        <p><b>Ga ikke Altinn deg muligheten til å gi tilgang?</b></p>
                        <p>Du kan gi tilgang hvis du har en av disse rollene:</p>
                        <ul style='line-height: 24px'>
                            <li>Du er registrert i Enhetsregisteret som daglig leder, styrets leder, bestyrende reder eller innehaver.</li>
                            <li>Du er registert som hovedadministrator i Altinn.</li>
                            <li>Du er «Tilgangsstyrer» i Altinn og har én, eller flere av rollene: «Rekruttering», «Lønn og personalmedarbeider», eller «Utfyller/innsender».</li>
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
