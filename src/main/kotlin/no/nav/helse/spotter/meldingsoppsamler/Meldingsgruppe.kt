package no.nav.helse.spotter.meldingsoppsamler

import no.nav.helse.spotter.meldingsoppsamler.Meldingsgruppe.MeldingMedIder.Companion.somMelding
import no.nav.helse.spotter.meldingsoppsamler.Meldingsgruppe.MeldingMedIder.Companion.somMeldingMedIder
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class Meldingsgruppe(melding: Melding) {
    private val id = UUID.randomUUID()
    private val meldinger = mutableListOf(melding.somMeldingMedIder())

    internal fun leggTil(melding: Melding): Meldingsgruppe? {
        if (meldinger.any { it.harId(melding.id) }) return this

        val meldingMedIder = melding.somMeldingMedIder()

        if (meldinger.none { it.kjennerTil(meldingMedIder) }) return null

        meldinger.add(meldingMedIder)
        logger.info("Meldingsgruppe[$id] inneholder nå ${meldinger.size} meldinger")
        return this
    }

    internal fun meldinger() = meldinger.map { it.somMelding() }.sortedBy { it.tidspunkt }

    internal fun oppdatertEtter(tidspunkt: LocalDateTime) = meldinger.any { it.oppdatertEtter(tidspunkt) }

    private class MeldingMedIder(
        private val id: UUID,
        private val navn: String,
        private val payload: String,
        private val tidspunkt: LocalDateTime) {
        private val ider = UuidLookup.lookup(payload).plus(id).toSet()
        private fun kjennerTil(ider: Set<UUID>) = this.ider.intersect(ider).isNotEmpty()
        fun harId(id: UUID) = this.id == id
        fun kjennerTil(melding: MeldingMedIder) = kjennerTil(melding.ider)
        fun oppdatertEtter(tidspunkt: LocalDateTime) = this.tidspunkt > tidspunkt
        companion object {
            fun Melding.somMeldingMedIder() = MeldingMedIder(
                id = id,
                tidspunkt = tidspunkt,
                navn = navn,
                payload = payload
            )
            fun MeldingMedIder.somMelding() = Melding(
                id = id,
                tidspunkt = tidspunkt,
                navn = navn,
                payload = payload
            )
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Meldingsgruppe::class.java)
    }
}