import org.junit.jupiter.api.Test
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class ElasticSearchTest {
    @Test
    fun testElasticsearch() {
        val elasticsearchUrl: String? = elasticsearch.httpHostAddress
        // Use the URL to configure your ES client
    }

    companion object {
        @Container
        private var elasticsearch: ElasticsearchContainer =
            ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.3")
                .withExposedPorts(9200)
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
    }
}