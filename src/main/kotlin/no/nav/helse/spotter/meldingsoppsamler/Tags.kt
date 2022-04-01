package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.eventName

internal object Tags {
    private fun JsonNode.isMissingOrNull() = isMissingNode || isNull

    internal fun Map<String, Any>.gjelderEnArbeidsgiver() = isNotEmpty() && typedTag<String>("behov.Godkjenning.innektskilde") == "EN_ARBEIDSGIVER"
    internal fun Map<String, Any>.gjelderFlereArbeidsgivere() = isNotEmpty() && typedTag<String>("behov.Godkjenning.innektskilde") == "FLERE_ARBEIDSGIVERE"
    internal fun Map<String, Any>.avventerArbeidgivereTilAvventerHistorikk() =
        isNotEmpty() && typedTag<String>("vedtaksperiode_endret.forrigeTilstand") == "AVVENTER_ARBEIDSGIVERE" && typedTag<String>("vedtaksperiode_endret.gjeldendeTilstand") == "AVVENTER_HISTORIKK"

    internal fun JsonNode.tags(): Map<String, Any> = when (eventName) {
        "behov" -> mapOf(
            "behov.Godkjenning.innektskilde" to godkjenningsbehovInntektskilde()
        ).filterNotNullValues()
        "vedtaksperiode_endret" -> mapOf(
            "vedtaksperiode_endret.forrigeTilstand" to vedtaksperiodeEndretTilstand("forrigeTilstand"),
            "vedtaksperiode_endret.gjeldendeTilstand" to vedtaksperiodeEndretTilstand("gjeldendeTilstand")
        ).filterNotNullValues()
        else -> emptyMap()
    }

    private fun <T> Map<String, Any>.typedTag(key: String) = get(key) as T?
    private fun Map<String, Any?>.filterNotNullValues(): Map<String, Any> = filterValues { it != null }.mapValues { it.value!! }

    private fun JsonNode.godkjenningsbehovInntektskilde() =
        path("Godkjenning").path("inntektskilde").takeUnless { it.isMissingOrNull() }?.asText()

    private fun JsonNode.vedtaksperiodeEndretTilstand(tilstandKey: String) =
        path(tilstandKey).takeUnless { it.isMissingOrNull() }?.asText()
}