package no.nav.toi.stilling.indekser

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.kotlinModule
import java.util.TimeZone

object JacksonConfig {
    val objectMapper: JsonMapper = JsonMapper.builder()
        // Jackson 3 avviser som standard null i f.eks. Map<String, String>; properties kan inneholde null
        .addModule(kotlinModule { disable(KotlinFeature.StrictNullChecks) })
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
        // Jackson 3 avviser null for Int som standard, Jackson 2 ga 0
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        // Properties som "2 stillinger" ble lest som 2 med Jackson 2. OpenSearch-klienten leser også dokumenter midt i en større respons.
        .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
        .build()
}
