package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.BooleanNode
import no.nav.helse.spotter.eventName

internal object Visningsnavn {
    internal fun JsonNode.visningsnavn() = when (eventName) {
        "behov" -> this.behovsnavn()
        else -> eventName
    }

    private fun JsonNode.behovsnavn() : String {
        if (erFinal()) return "løsning ${behovId()} (komplett)"
        val løsninger = løsninger()
        if (løsninger.isNotEmpty()) return "løsning ${behovId()} (${løsninger.joinToString(",")})"
        val behov = behov()
        if (behov.size == 1) return "behov   ${behovId()} (${behov.first()})"
        return "behov   ${behovId()} (${behov.size} stykk)"
    }

    private fun JsonNode.erFinal() =
        hasNonNull("@final") && get("@final") is BooleanNode && get("@final").asBoolean()

    private fun JsonNode.behov() = when (hasNonNull("@behov")) {
        true -> get("@behov").map { it.asText() }.formaterBehov()
        false -> emptyList()
    }

    private fun JsonNode.løsninger() = when (hasNonNull("@løsning")) {
        true -> get("@løsning").fieldNames().asSequence().toList().formaterBehov()
        false -> emptyList()
    }

    private fun List<String>.formaterBehov() = map { when {
        it.length <= 20 -> it
        else -> "${it.take(18)}.."
    }}

    private fun JsonNode.behovId() = when (hasNonNull("@behovId")) {
        true -> get("@behovId").asText().takeLast(3)
        false -> "n/a"
    }
}