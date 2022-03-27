package no.nav.helse.spotter.meldingsoppsamler

import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

internal class Melding(
    val id: UUID,
    val navn: String,
    val deltaker: Deltaker,
    val payload: String
) {
    override fun equals(other: Any?) = other is Melding && other.id == id
    override fun hashCode() = id.hashCode()
    override fun toString() = "$id"

    internal companion object {
        private fun String.pad() = padEnd(35, ' ')
        private fun Duration.formater() = "${toSeconds()} sekunder & ${toMillisPart()} millisekunder"
        private fun LocalDateTime.formater() = "$this".pad()
        internal fun List<Melding>.formatertTotaltid() = Duration.between(first().deltaker.tidspunkt, last().deltaker.tidspunkt).formater()
        internal fun List<Melding>.formater() : String {
            var forrigeTidspunkt: LocalDateTime? = null
            return "\n-> " + joinToString("\n-> ") { melding -> when (forrigeTidspunkt) {
                null -> "${melding.navn.pad()}${melding.id}\t${melding.deltaker.tidspunkt.formater()}${melding.deltaker.navn}"
                else -> "${melding.navn.pad()}${melding.id}\t${Duration.between(forrigeTidspunkt, melding.deltaker.tidspunkt).formater().pad()}${melding.deltaker.navn}"
            }.also { forrigeTidspunkt = melding.deltaker.tidspunkt }}
        }
    }
}