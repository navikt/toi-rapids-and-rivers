package no.nav.toi.stilling.indekser

import org.opensearch.testcontainers.OpensearchContainer
import org.testcontainers.utility.DockerImageName

class OpenSearchContainer {

    val container: OpensearchContainer<*> = OpensearchContainer(OPENSEARCH_IMAGE).withSecurityEnabled()

    companion object {
        private val OPENSEARCH_IMAGE = DockerImageName.parse("opensearchproject/opensearch:2.11.0")
    }

    init {
        container.start()
    }
}
