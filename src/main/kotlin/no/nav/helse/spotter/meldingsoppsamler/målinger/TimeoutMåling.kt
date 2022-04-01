package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.Melding
import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.målinger.Måling.Companion.målingFerdig

internal object TimeoutMåling : MeldingsgruppeListener {
    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>): Boolean {
        if (meldinger.isEmpty()) return false // Dette burde jo ikke skje, men guard for bruk av first() & last() under
        val navn = "${meldinger.first().navn}_til_${meldinger.last().navn}"
        meldinger.målingFerdig(navn)
        return true
    }
}