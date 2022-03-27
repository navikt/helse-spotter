package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.Melding
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formater
import org.slf4j.LoggerFactory

internal abstract class Måling(
    private val navn : String,
    private val fra: (melding: Melding) -> Boolean,
    private val til: (melding: Melding) -> Boolean,
    private val erAktuell: (måling: List<Melding>) -> Boolean = { true }
) : MeldingsgruppeListener {

    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>): Boolean{
        if (!til(nyMelding)) return false
        val fraIndex = meldinger.indexOfLastOrNull(fra) ?: return false
        val tilIndex = meldinger.indexOfLastOrNull(til) ?: return false
        if (fraIndex > tilIndex) return false
        val måling = meldinger.subList(fraIndex, tilIndex + 1)
        if (!erAktuell(måling)) return false
        logger.info(måling.formater("Måling $navn tok"))
        return true
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(Måling::class.java)

        private fun List<Melding>.indexOfLastOrNull(predicate: (melding: Melding) -> Boolean): Int? {
            val index = indexOfLast(predicate)
            if (index < 0) return null
            return index
        }
    }
}