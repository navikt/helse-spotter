package no.nav.helse.spotter.meldingsoppsamler

import java.time.LocalDateTime
import java.util.UUID

internal class Meldingsgruppe(melding: Melding) {
    private var sistOppdatert: LocalDateTime = melding.deltaker.tidspunkt
    private val meldinger = mutableListOf<Melding>()
    private val ider = mutableSetOf<UUID>()

    private fun add(melding: Melding) {
        ider.addAll(melding.ider)
        meldinger.add(melding)
        if (melding.deltaker.tidspunkt > sistOppdatert) {
            sistOppdatert = melding.deltaker.tidspunkt
        }
    }

    init { add(melding) }

    internal fun leggTil(melding: Melding): Meldingsgruppe? {
        if (ider.intersect(melding.ider).isEmpty()) return null
        add(melding)
        return this
    }

    internal fun meldinger() = meldinger.sortedBy { it.deltaker.tidspunkt }
    internal fun oppdatertEtter(tidspunkt: LocalDateTime) = sistOppdatert > tidspunkt
}