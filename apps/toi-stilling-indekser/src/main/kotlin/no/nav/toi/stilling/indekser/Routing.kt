package no.nav.toi.stilling.indekser

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/isalive") {
            if(Liveness.isAlive) VersionCheckResult.OK else call.response.status(HttpStatusCode.ServiceUnavailable)
        }
    }
}
