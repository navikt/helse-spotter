package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.eventName

internal object Tags {
    private fun JsonNode.isMissingOrNull() = isMissingNode || isNull
    private const val UNDEFINED = "__UNDEFINED__"

    internal fun Map<String, Any>.gjelderEnArbeidsgiver() = typedTag<String>("behov.Godkjenning.innektskilde") == "EN_ARBEIDSGIVER"
    internal fun Map<String, Any>.gjelderFlereArbeidsgivere() = typedTag<String>("behov.Godkjenning.innektskilde") == "FLERE_ARBEIDSGIVERE"
    internal fun Map<String, Any>.avventerArbeidgivereTilAvventerHistorikk() =
        typedTag<String>("vedtaksperiode_endret.forrigeTilstand") == "AVVENTER_ARBEIDSGIVERE" && typedTag<String>("vedtaksperiode_endret.gjeldendeTilstand") == "AVVENTER_HISTORIKK"

    internal fun JsonNode.tags(): Map<String, Any> = mapOf(
        "behov.Godkjenning.innektskilde" to godkjenningsbehovInntektskilde(),
        "vedtaksperiode_endret.forrigeTilstand" to vedtaksperiodeEndretTilstand("forrigeTilstand"),
        "vedtaksperiode_endret.gjeldendeTilstand" to vedtaksperiodeEndretTilstand("gjeldendeTilstand")
    )

    private fun <T> Map<String, Any>.typedTag(key: String) = getValue(key) as T

    private fun JsonNode.godkjenningsbehovInntektskilde() =
        takeIf { eventName == "behov" }?.path("Godkjenning")?.path("inntektskilde")?.takeUnless { it.isMissingOrNull() }?.asText() ?: UNDEFINED

    private fun JsonNode.vedtaksperiodeEndretTilstand(tilstandKey: String) =
        takeIf { eventName == "vedtaksperiode_endret" }?.path(tilstandKey)?.takeUnless { it.isMissingOrNull() }?.asText() ?: UNDEFINED
}