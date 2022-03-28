package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.Melding
import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.målinger.Måling.Companion.målingFerdig

internal object TimeoutMåling : MeldingsgruppeListener {
    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>): Boolean {
        if (meldinger.size < 2 || meldinger.first().navn == meldinger.last().navn || meldinger.last().navn == "behov") return false
        val navn = "${meldinger.first().navn}_til_${meldinger.last().navn}"
        meldinger.målingFerdig(navn)
        return true
    }
}