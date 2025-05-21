package no.nav.toi.stilling.indekser

object Liveness {
    var isAlive = true
        private set

    fun kill(årsak: String, exception: Throwable) {
        log.error(årsak, exception)
        isAlive = false
    }
}
