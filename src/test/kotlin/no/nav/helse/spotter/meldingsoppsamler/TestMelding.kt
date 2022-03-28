package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

internal class TestMelding(
    id: UUID,
    navn: String,
    deltaker: Deltaker = Deltaker(tidspunkt = LocalDateTime.now(), navn = "test"),
    ider: Set<UUID> = emptySet(),
    extra: String = "ekstra data i meldingen") {

    @Language("JSON")
    internal val melding = Melding(jackson.readTree("""
    {
      "@id": "$id",
      "@event_name": "$navn",
      "system_participating_services": [{
        "service": "${deltaker.navn}",
        "time": "${deltaker.tidspunkt}"
      }],
      "ider": ${ider.map { "\"$it\"" }},
      "extra": "$extra"
    }
    """))

    private companion object {
        val jackson = jacksonObjectMapper()
    }
}