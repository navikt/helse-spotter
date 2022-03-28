package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.helse.spotter.meldingsoppsamler.Deltaker.Companion.antallDeltakere

internal object TagResolver {
    internal fun JsonNode.resolveTags(): Map<String, Any> {
        val payload = "$this"
        return mapOf(
            "antallDeltakere" to antallDeltakere(),
            "enArbeidsgiver" to payload.contains("EN_ARBEIDSGIVER"),
            "flereArbeidsgivere" to payload.contains("FLERE_ARBEIDSGIVERE"),
            "@final" to hasNonNull("@final"),
            "etterspurteBehov" to ettespurteBehov()
        )
    }
    internal fun <T>Map<String, Any>.typedTag(key: String) = getValue(key) as T
    private fun JsonNode.ettespurteBehov() = when (hasNonNull("@behov") && get("@behov") is ArrayNode) {
        true -> get("@behov").map { it.asText() }
        false -> emptyList()
    }
}