package no.nav.toi.stilling.indekser

import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.*

class OpenSearchContainer {

    val container: ElasticsearchContainer = ElasticsearchContainer(
        DockerImageName.parse("opensearchproject/opensearch:2.11.0")
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
    ).withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", OPENSEARCH_PASSWORD)
        .withEnv("plugins.security.disabled", "true")
        .withEnv("discovery.type", "single-node")

    val username: String = "admin"
    val password: String = OPENSEARCH_PASSWORD

    companion object {
        private const val OPENSEARCH_PASSWORD = "Admin@12345"
    }

    init {
        container.start()
        // Wait for the container to be ready by repeatedly checking connectivity
        waitForReady()
    }

    private fun waitForReady(maxAttempts: Int = 120, delayMs: Long = 2000) {
        var attempts = 0
        var lastError: Exception? = null
        while (attempts < maxAttempts) {
            try {
                // Try to access the health endpoint without auth first
                val url = java.net.URI("http://${container.httpHostAddress}/_cluster/health").toURL()
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.instanceFollowRedirects = true
                try {
                    val responseCode = connection.responseCode
                    if (responseCode in 200..299) {
                        // Container is ready
                        return
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: java.net.SocketTimeoutException) {
                // Container not ready yet
                lastError = e
            } catch (e: java.net.ConnectException) {
                // Container not ready yet
                lastError = e
            } catch (e: Exception) {
                // Log but continue
                lastError = e
            }
            Thread.sleep(delayMs)
            attempts++
        }
        throw RuntimeException("Timed out waiting for OpenSearch container to be ready after $maxAttempts attempts (${maxAttempts * delayMs}ms). Last error: ${lastError?.message ?: "unknown"}")
    }
}
