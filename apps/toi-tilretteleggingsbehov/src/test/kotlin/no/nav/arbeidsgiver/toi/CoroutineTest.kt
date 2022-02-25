package no.nav.arbeidsgiver.toi

import kotlinx.coroutines.*

suspend fun main() {                                // A function that can be suspended and resumed later
    val start = System.currentTimeMillis()
    coroutineScope {                                // Create a scope for starting coroutines
        for (i in 1..3000) {
            launch {                                // Start 10 concurrent tasks
                (0..(3000000L - i*1000)).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong).map(Long::toString).map(String::toLong)              // Pause their execution
                log(start, "Countdown: $i")
            }
        }
    }
    // Execution continues when all coroutines in the scope have finished
    log(start, "Liftoff!")
}

fun log(start: Long, msg: String) {
    println("$msg " +
            "(on ${Thread.currentThread().name}) " +
            "after ${(System.currentTimeMillis() - start)/1000F}s")
}