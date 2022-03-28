package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.Melding
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formater
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formatertTotaltid
import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import org.slf4j.LoggerFactory

internal object TimeoutMåling : MeldingsgruppeListener {
    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>): Boolean {
        if (meldinger.size < 2 || meldinger.first().navn == meldinger.last().navn) return false
        val navn = "${meldinger.first().navn}_til_${meldinger.last().navn}"
        logger.info("Måling $navn tok ${meldinger.formatertTotaltid()} ${meldinger.formater()}")
        return true
    }

    private val logger = LoggerFactory.getLogger(Måling::class.java)
}