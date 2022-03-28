package no.nav.helse.spotter.meldingsoppsamler

import java.time.LocalDateTime

internal class Meldingsgruppe(melding: Melding) {
    private val meldinger = mutableListOf(melding)

    internal fun leggTil(melding: Melding): Meldingsgruppe? {
        if (meldinger.none { it.kjennerTil(melding) }) return null
        meldinger.add(melding)
        return this
    }

    internal fun meldinger() = meldinger.sortedBy { it.deltaker.tidspunkt }
    internal fun oppdatertEtter(tidspunkt: LocalDateTime) = meldinger.any { it.oppdatertEtter(tidspunkt) }

    private fun Melding.kjennerTil(melding: Melding) = this.ider.intersect(melding.ider).isNotEmpty()
    private fun Melding.oppdatertEtter(tidspunkt: LocalDateTime) = this.deltaker.tidspunkt > tidspunkt
}