package no.nav.helse.spotter.meldingsoppsamler

import java.time.LocalDateTime
import java.util.UUID

internal class Meldingsgruppe(melding: Melding) {
    private val meldinger = mutableListOf(melding)
    private val ider = mutableSetOf<UUID>().also { it.addAll(melding.ider) }

    internal fun leggTil(melding: Melding): Meldingsgruppe? {
        if (ider.intersect(melding.ider).isEmpty()) return null
        meldinger.add(melding)
        ider.addAll(melding.ider)
        return this
    }

    internal fun meldinger() = meldinger.sortedBy { it.deltaker.tidspunkt }
    internal fun oppdatertEtter(tidspunkt: LocalDateTime) = meldinger.any { it.oppdatertEtter(tidspunkt) }

    private fun Melding.oppdatertEtter(tidspunkt: LocalDateTime) = this.deltaker.tidspunkt > tidspunkt
}