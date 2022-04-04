package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.Melding
import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.målinger.Måling.Companion.målingFerdig
import org.slf4j.LoggerFactory

internal object TimeoutMåling : MeldingsgruppeListener {
    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>): Boolean {
        if (meldinger.size <= 1) return false
        val fraNavn = meldinger.first().navn
        val tilNavn = meldinger.last().navn
        if (fraNavn == tilNavn) return false
        val navn = "${fraNavn}_til_${tilNavn}"
        meldinger.målingFerdig(navn, logger)
        return true
    }

    private val logger = LoggerFactory.getLogger(TimeoutMåling::class.java)
}