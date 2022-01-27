package no.nav.helse.spotter

import no.nav.helse.spotter.måling.Måling
import no.nav.helse.spotter.måling.OverstyrInntektMåling

internal class Målinger {
    private val målinger = mutableListOf<Måling>()

    internal fun håndter(måling: Måling) {
        // Håndtere melding på pågående målinger
        //målinger.forEach { it.håndter(melding) }
        // Nye målinger

        //OverstyrInntektMåling(melding).leggTilOmStartet()
    }

    private fun Måling.leggTilOmStartet() {
        if (state() != Måling.State.MålingStartet) return
        målinger.add(this)
    }
}