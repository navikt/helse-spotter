package no.nav.helse.spotter.meldingsoppsamler

import no.nav.helse.spotter.meldingsoppsamler.Meldingsgruppe.MeldingMedIder.Companion.somMelding
import no.nav.helse.spotter.meldingsoppsamler.Meldingsgruppe.MeldingMedIder.Companion.somMeldingMedIder
import java.time.LocalDateTime
import java.util.*

internal class Meldingsgruppe(melding: Melding) {
    private val meldinger = mutableListOf(melding.somMeldingMedIder())

    internal fun leggTil(melding: Melding): Meldingsgruppe? {
        val meldingMedIder = melding.somMeldingMedIder()

        if (meldinger.none { it.kjennerTil(meldingMedIder) }) return null

        meldinger.add(meldingMedIder)
        return this
    }

    internal fun meldinger() = meldinger.map { it.somMelding() }.sortedBy { it.deltaker.tidspunkt }

    internal fun oppdatertEtter(tidspunkt: LocalDateTime) = meldinger.any { it.oppdatertEtter(tidspunkt) }

    private class MeldingMedIder(
        private val id: UUID,
        private val navn: String,
        private val payload: String,
        private val deltaker: Deltaker) {
        private val ider = UuidLookup.lookup(payload).plus(id).toSet()
        private fun kjennerTil(ider: Set<UUID>) = this.ider.intersect(ider).isNotEmpty()
        fun kjennerTil(melding: MeldingMedIder) = kjennerTil(melding.ider)
        fun oppdatertEtter(tidspunkt: LocalDateTime) = this.deltaker.tidspunkt > tidspunkt
        companion object {
            fun Melding.somMeldingMedIder() = MeldingMedIder(
                id = id,
                deltaker = deltaker,
                navn = navn,
                payload = payload
            )
            fun MeldingMedIder.somMelding() = Melding(
                id = id,
                deltaker = deltaker,
                navn = navn,
                payload = payload
            )
        }
    }
}