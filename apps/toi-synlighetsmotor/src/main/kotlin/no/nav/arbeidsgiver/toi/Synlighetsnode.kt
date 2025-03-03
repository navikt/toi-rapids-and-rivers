package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper


interface Synlighetsnode<T>{
    fun hvisIkkeNullOg(condition: (T) -> Boolean): BooleanVerdi
    fun hvisNullEller(condition: (T) -> Boolean): BooleanVerdi
    fun svarPåDetteFeltetLiggerPåHendelse(): Boolean
    fun verdiEllerNull(): T?
    fun hvisMissingEller(condition: (T?) -> Boolean): Boolean

    companion object {
        inline fun <reified T> fromJsonNode(node: JsonNode, objectMapper: ObjectMapper) = when {
            node.isMissingNode -> MissingNode()
            node.isNull -> NullNode()
            else -> SynligNode(objectMapper.treeToValue(node, T::class.java))
        }
    }
}

class SynligNode<T>(
    private val value: T
): Synlighetsnode<T> {
    override fun hvisIkkeNullOg(condition: (T) -> Boolean) = condition(value).tilBooleanVerdi()
    override fun hvisNullEller(condition: (T) -> Boolean) = condition(value).tilBooleanVerdi()
    override fun svarPåDetteFeltetLiggerPåHendelse() = true
    override fun verdiEllerNull() = value
    override fun hvisMissingEller(condition: (T?) -> Boolean) = condition(value)

    override fun toString() = value.toString()
}
class MissingNode<T>: Synlighetsnode<T> {
    override fun hvisIkkeNullOg(condition: (T) -> Boolean) = BooleanVerdi.missing
    override fun hvisNullEller(condition: (T) -> Boolean) = BooleanVerdi.missing
    override fun svarPåDetteFeltetLiggerPåHendelse() = false
    override fun verdiEllerNull() = null
    override fun hvisMissingEller(condition: (T?) -> Boolean) = true

    override fun toString() = "MissingNode"
}
class NullNode<T>: Synlighetsnode<T> {
    override fun hvisIkkeNullOg(condition: (T) -> Boolean) = false.tilBooleanVerdi()
    override fun hvisNullEller(condition: (T) -> Boolean) = true.tilBooleanVerdi()
    override fun svarPåDetteFeltetLiggerPåHendelse() = true
    override fun verdiEllerNull() = null
    override fun hvisMissingEller(condition: (T?) -> Boolean) = condition(null)

    override fun toString() = "NullNode"
}