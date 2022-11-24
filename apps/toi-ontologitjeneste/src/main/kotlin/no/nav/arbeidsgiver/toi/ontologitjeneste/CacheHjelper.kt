package no.nav.arbeidsgiver.toi.ontologitjeneste

import org.ehcache.CacheManager
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.MemoryUnit
import java.util.*

class CacheHjelper {
    private val cacheKonfigurasjon = CacheConfigurationBuilder.newCacheConfigurationBuilder(
        String::class.java, OntologiRelasjoner::class.java,
        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(200, MemoryUnit.MB)
    )
    private val ontologiCache = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache(
            "preConfiguredCache",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String::class.java, OntologiRelasjoner::class.java,
                ResourcePoolsBuilder.heap(200)
            )
        ).build().also(CacheManager::init)

    fun lagCache(getter: (String) -> OntologiRelasjoner): (String) -> OntologiRelasjoner =
        ontologiCache.createCache(
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