package no.nav.arbeidsgiver.toi.identmapper

class PdlKlient(private val accessTokenClient: AccessTokenClient) {

    fun aktørIdFor(fødselsnummer: String): String {
        val accessToken = accessTokenClient.hentAccessToken()


    }
}
