package no.nav.arbeidsgiver.toi.ontologitjeneste

import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import java.util.*

class CacheHjelper {
    private val cacheKonfigurasjon = CacheConfigurationBuilder.newCacheConfigurationBuilder(
        String::class.java, OntologiRelasjoner::class.java,
        ResourcePoolsBuilder.heap(200)
    )

    private val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache(
            "preConfiguredCache",
            cacheKonfigurasjon
        ).build().also(CacheManager::init)

    fun lagCache(getter: (String) -> OntologiRelasjoner): (String) -> OntologiRelasjoner =
        cacheManager.createCache(
            "cache${UUID.randomUUID()}",
            cacheKonfigurasjon
        ).let { cache ->
            { key ->
                if (!cache.containsKey(key)) {
                    cache.put(key, getter(key))
                }
                cache.get(key)
            }
        }
}
