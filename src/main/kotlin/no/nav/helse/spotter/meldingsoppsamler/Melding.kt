package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.eventName
import no.nav.helse.spotter.id
import no.nav.helse.spotter.meldingsoppsamler.Deltaker.Companion.deltaker
import no.nav.helse.spotter.meldingsoppsamler.Tags.tags
import no.nav.helse.spotter.meldingsoppsamler.UuidLookup.uuids
import no.nav.helse.spotter.meldingsoppsamler.Visningsnavn.visningsnavn
import java.time.Duration
import java.time.LocalDateTime

internal class Melding(json: JsonNode) {
    internal val id = json.id
    internal val navn = json.eventName
    private val visningsnavn = json.visningsnavn()
    internal val deltaker = json.deltaker()
    internal val tags = json.tags()
    internal val ider = json.uuids()

    override fun equals(other: Any?) = other is Melding && other.id == id
    override fun hashCode() = id.hashCode()
    override fun toString() = "$id"

    internal companion object {
        private fun String.pad() = padEnd(35, ' ').take(35)
        private fun LocalDateTime.formater() = "$this".pad()

        internal fun Duration.formaterDuration() = "${toSeconds()} sekunder & ${toMillisPart()} millisekunder"
        internal fun List<Melding>.totaltid() = Duration.between(first().deltaker.tidspunkt, last().deltaker.tidspunkt)
        internal fun List<Melding>.formater() : String {
            var forrigeTidspunkt: LocalDateTime? = null
            return "\n-> " + joinToString("\n-> ") { melding -> when (forrigeTidspunkt) {
                null -> "${melding.visningsnavn.pad()}${melding.id}\t${melding.deltaker.tidspunkt.formater()}${melding.deltaker.navn}"
                else -> "${melding.visningsnavn.pad()}${melding.id}\t${Duration.between(forrigeTidspunkt, melding.deltaker.tidspunkt).formaterDuration().pad()}${melding.deltaker.navn}"
            }.also { forrigeTidspunkt = melding.deltaker.tidspunkt }}
        }
    }
}