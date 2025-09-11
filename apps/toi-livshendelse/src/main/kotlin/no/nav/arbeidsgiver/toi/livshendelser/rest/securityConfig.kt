package no.nav.arbeidsgiver.toi.livshendelser.rest

import no.nav.security.token.support.core.configuration.IssuerProperties
import java.net.URI

fun hentIssuerProperties(envs: Map<String, String>) =
    mapOf(
        Rolle.VEILEDER to
                (envs["AZURE_OPENID_CONFIG_ISSUER"]!! to
                IssuerProperties(
                    discoveryUrl = URI(envs["AZURE_APP_WELL_KNOWN_URL"]!!).toURL(),
                    acceptedAudience = listOf(envs["AZURE_APP_CLIENT_ID"]!!)
                ))
    )
