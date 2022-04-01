package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal object UuidLookup {
    private val UUID_REGEX = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b".toRegex()
    internal fun lookup(tekst: String) = UUID_REGEX.findAll(tekst).map { UUID.fromString(it.value) }.toSet()
    internal fun JsonNode.uuids() = lookup("$this")
}