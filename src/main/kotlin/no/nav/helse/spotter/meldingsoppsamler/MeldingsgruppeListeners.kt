package no.nav.helse.spotter.meldingsoppsamler

import java.util.UUID

internal class MeldingsgruppeListeners(
    private val listeners: List<MeldingsgruppeListener>,
    private val timeoutListener: MeldingsgruppeListener) {
    private val håndterte = mutableSetOf<UUID>()

    internal fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>) {
        if (nyMelding.allerdeHåndtert()) return

        if (minstEnListenerHarHåndtert(nyMelding, meldinger)) {
            nyMelding.håndtert()
        }
    }

    internal fun onTimeout(meldinger: List<Melding>) {
        val sisteMelding = meldinger.lastOrNull() ?: return
        if (sisteMelding.allerdeHåndtert()) return
        timeoutListener.onNyMelding(sisteMelding, meldinger)
        håndterte.removeAll(meldinger.map { it.id }.toSet())
    }

    private fun Melding.allerdeHåndtert() = håndterte.contains(this.id)
    private fun Melding.håndtert() = håndterte.add(this.id)
    private fun minstEnListenerHarHåndtert(nyMelding: Melding, meldinger: List<Melding>) =
        listeners.map { listener -> listener.onNyMelding(nyMelding, meldinger) }.any { it }
}