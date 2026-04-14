package no.nav.toi.stilling.indekser

import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

class OpenSearchContainer {

    val container: ElasticsearchContainer = ElasticsearchContainer(
        DockerImageName.parse("opensearchproject/opensearch:2.11.0")
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
    ).withEnv("plugins.security.disabled", "true")
        .withEnv("discovery.type", "single-node")

    init {
        container.start()
    }
}
