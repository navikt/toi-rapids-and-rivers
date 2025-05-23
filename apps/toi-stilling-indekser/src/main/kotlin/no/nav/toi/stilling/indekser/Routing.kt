package no.nav.toi.stilling.indekser

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/isalive") {
            if((!Liveness.isAlive && !rapidIsAlive.invoke())) {
                log.info("Appen skal starte på nytt, isalive er false")
            }
            if(Liveness.isAlive && rapidIsAlive.invoke()) call.respondText("ALIVE", status = HttpStatusCode.OK) else call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }
}
