package no.nav.arbeidsgiver.toi.livshendelser

import no.nav.person.pdl.leesah.Personhendelse

fun List<Personhendelse>.håndter() {
    this.filter{it.opplysningstype == "ADRESSEBESKYTTELSE"}
        .map { it.personidenter.firstOrNull() }
        .forEach { it?.let { håndterDiskresjonskodeEndringPåIdent(it) } ?: log.error("Ingen personidenter funnet på hendelse") }
}

fun håndterDiskresjonskodeEndringPåIdent(ident: String): Unit  = TODO("Kall mot pdl, publiser på rapid")
