package no.nav.toi.stilling.indekser

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/isalive") {
            log.info("IsAlive er kalt")
            if(Liveness.isAlive) call.respondText("ALIVE", status = HttpStatusCode.OK) else call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }
}
