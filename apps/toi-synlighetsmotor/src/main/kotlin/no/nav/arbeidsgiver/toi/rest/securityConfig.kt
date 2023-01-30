package no.nav.arbeidsgiver.toi.rest

import no.nav.security.token.support.core.configuration.IssuerProperties
import java.net.URL

fun hentIssuerProperties(envs: Map<String, String>) =
    mapOf(
        Rolle.VEILEDER to
                IssuerProperties(
                    URL(envs["AZURE_APP_WELL_KNOWN_URL"]),
                    listOf(envs["AZURE_APP_CLIENT_ID"]),
                    envs["AZURE_OPENID_CONFIG_ISSUER"]
                )
    )
