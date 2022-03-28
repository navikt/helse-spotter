package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.eventName

internal object Visningsnavn {
    internal fun JsonNode.visningsnavn() = when (eventName) {
        "behov" -> this.behovsnavn()
        else -> eventName
    }

    private fun JsonNode.behovsnavn() : String {
        if (hasNonNull("@final")) return "løsning (komplett)"
        val løsninger = løsninger()
        if (løsninger.isEmpty()) return "behov"
        return "løsning (${løsninger.joinToString(",")})"
    }

    private fun JsonNode.løsninger() = when (hasNonNull("@løsning")) {
        true -> get("@løsning").fieldNames().asSequence().toList()
        false -> emptyList()
    }
}