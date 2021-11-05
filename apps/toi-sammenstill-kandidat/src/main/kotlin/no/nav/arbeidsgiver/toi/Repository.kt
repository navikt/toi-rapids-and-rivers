package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import no.nav.helse.rapids_rivers.JsonMessage
import org.bson.Document

class Repository(mongoClient: MongoClient) {

    val db = mongoClient.getDatabase("sammenstilteKandidater")
    val collection = db.getCollection("kandidater")

    fun lagreKandidat(kandidat: Kandidat) {
        collection.insertOne(Document.parse(kandidat.toJson()))
    }

    fun hentKandidat(aktørId: String): Kandidat? {
        val document = collection.find(Filters.eq("kandidat.aktørId", aktørId)).firstOrNull() ?: return null
        return jacksonObjectMapper().readValue(document.toJson(), Kandidat::class.java)
    }
}

typealias AktøridHendelse = Pair<String, JsonMessage>


