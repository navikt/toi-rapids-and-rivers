package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun offsetTidsmodul(): SimpleModule {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val zone = ZoneId.of("Europe/Oslo")

    class YearMonthSerializer : StdSerializer<YearMonth>(YearMonth::class.java) {
        override fun serialize(value: YearMonth, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(value.atDay(1).atStartOfDay(zone).toOffsetDateTime().format(formatter))
        }
    }
    class YearMonthDeserializer : StdDeserializer<YearMonth>(YearMonth::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
            YearMonth.from(OffsetDateTime.parse(p.text.trim(), formatter))
    }

    class LocalDateSerializer : StdSerializer<LocalDate>(LocalDate::class.java) {
        override fun serialize(value: LocalDate, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(value.atStartOfDay(zone).toOffsetDateTime().format(formatter))
        }
    }

    class LocalDateDeserializer : StdDeserializer<LocalDate>(LocalDate::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
            OffsetDateTime.parse(p.text.trim(), formatter).toLocalDate()
    }

    class OffsetDateTimeSerializer : StdSerializer<OffsetDateTime>(OffsetDateTime::class.java) {
        override fun serialize(value: OffsetDateTime, gen: JsonGenerator, provider: SerializerProvider) {
            gen.writeString(value.atZoneSameInstant(zone).format(formatter))
        }
    }

    class OffsetDateTimeDeserializer : StdDeserializer<OffsetDateTime>(OffsetDateTime::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext) =
            OffsetDateTime.parse(p.text.trim(), formatter)
    }

    return SimpleModule()
        .addSerializer(YearMonth::class.java, YearMonthSerializer())
        .addDeserializer(YearMonth::class.java, YearMonthDeserializer())
        .addSerializer(LocalDate::class.java, LocalDateSerializer())
        .addDeserializer(LocalDate::class.java, LocalDateDeserializer())
        .addSerializer(OffsetDateTime::class.java, OffsetDateTimeSerializer())
        .addDeserializer(OffsetDateTime::class.java, OffsetDateTimeDeserializer())
}