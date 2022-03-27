package no.nav.helse.spotter.meldingsoppsamler.målinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotter.meldingsoppsamler.Deltaker.Companion.antallDeltakere

private val objectMapper = jacksonObjectMapper()
private fun String.jsonNode() = objectMapper.readTree(this)
private fun JsonNode.ettespurteBehov() = when (hasNonNull("@behov") && get("@behov") is ArrayNode) {
    true -> get("@behov").map { it.asText() }
    false -> emptyList()
}

internal object OverstyrTidslinje : Måling(
    navn = "Overstyring av tidslinje",
    fra = { it.navn == "overstyr_tidslinje" },
    til = { it.navn == "oppgave_opprettet" }
)

internal object OverstyrInntekt : Måling(
    navn = "Overstyring av inntekt",
    fra = { it.navn == "overstyr_inntekt" },
    til = { it.navn == "oppgave_opprettet" }
)

internal object GodkjenningEnArbeidsgiver : Måling(
    navn = "Godkjenning av en arbeidsgiver",
    fra = { it.navn == "saksbehandler_løsning" },
    til = { it.navn == "oppgave_opprettet" },
    erAktuell = { måling -> måling.any { it.payload.contains("EN_ARBEIDSGIVER") }
})

internal object GodkjenningFlereArbeidsgivere : Måling(
    navn = "Godkjenning av flere arbeidsgivere",
    fra = { it.navn == "saksbehandler_løsning" },
    til = { it.navn == "oppgave_opprettet" },
    erAktuell = { måling -> måling.any { it.payload.contains("FLERE_ARBEIDGIVERE") }
})

internal object LøseBehovForHistorikk : Måling(
    navn = "Løse behov for historikk",
    fra = { it.navn == "behov" && it.payload.jsonNode().let { json -> json.ettespurteBehov().contains("Foreldrepenger") && json.antallDeltakere() == 1 }},
    til = { it.navn == "behov" && it.payload.jsonNode().let { json -> json.ettespurteBehov().contains("Foreldrepenger") && json.hasNonNull("@final") }}
)