package no.nav.toi.stilling.publiser.arbeidsplassen

import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenStilling

class ArbeidsplassenRestKlientMock: ArbeidsplassenRestKlient {
    override fun publiserStilling(stilling: ArbeidsplassenStilling) {
        println("Publiserer stilling til Arbeidsplassen: $stilling")
    }
}
