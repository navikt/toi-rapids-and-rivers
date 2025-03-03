package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


interface Synlighetsnode<T>{
    fun hvisFinnesOg(condition: (T) -> Boolean): Boolean
    fun hvisNullEller(condition: (T) -> Boolean): Boolean
    fun svarPåDetteFeltetLiggerPåHendelse(): Boolean
    fun verdiEllerNull(): T?

    companion object {
        inline fun <reified T> fromJsonNode(node: JsonNode) = when {
            node.isMissingNode -> MissingNode()
            node.isNull -> NullNode()
            else -> SynligNode(jacksonObjectMapper().treeToValue(node, T::class.java))
        }
    }
}

class SynligNode<T>(
    private val value: T
): Synlighetsnode<T> {
    override fun hvisFinnesOg(condition: (T) -> Boolean) = condition(value)
    override fun hvisNullEller(condition: (T) -> Boolean) = condition(value)
    override fun svarPåDetteFeltetLiggerPåHendelse() = true
    override fun verdiEllerNull() = value

    override fun toString() = value.toString()
}
class MissingNode<T>: Synlighetsnode<T> {
    override fun hvisFinnesOg(condition: (T) -> Boolean) = false
    override fun hvisNullEller(condition: (T) -> Boolean) = false
    override fun svarPåDetteFeltetLiggerPåHendelse() = false
    override fun verdiEllerNull() = null
    override fun toString() = "MissingNode"
}
class NullNode<T>: Synlighetsnode<T> {
    override fun hvisFinnesOg(condition: (T) -> Boolean) = false
    override fun hvisNullEller(condition: (T) -> Boolean) = true
    override fun svarPåDetteFeltetLiggerPåHendelse() = true
    override fun verdiEllerNull() = null
    override fun toString() = "NullNode"
}