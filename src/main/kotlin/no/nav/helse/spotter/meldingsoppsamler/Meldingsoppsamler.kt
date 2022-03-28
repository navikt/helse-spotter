package no.nav.helse.spotter.meldingsoppsamler

import java.time.LocalDateTime

internal class Meldingsoppsamler(
    private val timeoutListener: MeldingsgruppeListener,
    private val listeners: List<MeldingsgruppeListener>,
    private val rydde: (tidspunkt: LocalDateTime) -> Boolean = { it.minute % 5 == 0 }
) {
    private val meldingsgrupper = mutableListOf<Meldingsgruppe>()

    internal fun leggTil(melding: Melding) {
        val eksisterendeKobledeMeldinger = meldingsgrupper.mapNotNull { it.leggTil(melding) }.onEach { eksisterende ->
            listeners.forEach { listener -> listener.onNyMelding(melding, eksisterende.meldinger()) }
        }
        if (eksisterendeKobledeMeldinger.isNotEmpty()) return finalize(melding)

        val ny = Meldingsgruppe(melding).also { meldingsgrupper.add(it) }
        listeners.forEach { listener -> listener.onNyMelding(melding, ny.meldinger()) }

        finalize(melding)
    }

    internal fun antallMeldingsgrupper() = meldingsgrupper.size

    private fun finalize(melding: Melding) {
        val tidspunkt = melding.deltaker.tidspunkt.minusMinutes(10)
        if (!rydde(tidspunkt)) return
        meldingsgrupper.removeIf { (!it.oppdatertEtter(tidspunkt)).also { slettes -> if (slettes) {
            it.meldinger().let { meldinger -> timeoutListener.onNyMelding(meldinger.last(), meldinger) }
        }}}
    }
}