package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class Synlighetsnode<T>(
    val isMissing: Boolean,
    val isNull: Boolean,
    val value: T?
)

inline fun <reified T> ObjectMapper.fromSynlighetsnode(node: JsonNode): Synlighetsnode<T> =
    Synlighetsnode(
        node.isMissingNode,
        node.isNull,
        if (node.isNull || node.isMissingNode) null
        else treeToValue(node, T::class.java)
    )

