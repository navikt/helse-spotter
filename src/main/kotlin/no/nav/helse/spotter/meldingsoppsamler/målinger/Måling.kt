package no.nav.helse.spotter.meldingsoppsamler.målinger

import io.prometheus.client.Histogram
import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.Melding
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formater
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formaterDuration
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.totaltid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal abstract class Måling(
    private val navn: (måling: List<Melding>) -> String,
    private val fra: (melding: Melding) -> Boolean,
    private val til: (melding: Melding) -> Boolean,
    private val erAktuell: (måling: List<Melding>) -> Boolean = { true }
) : MeldingsgruppeListener {

    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>): Boolean{
        if (!til(nyMelding)) return false
        val fraIndex = meldinger.indexOfLastOrNull(fra) ?: return false
        val tilIndex = meldinger.filterIndexed { index, _ -> index >= fraIndex }.indexOfFirstOrNull(til) ?: return false
        val måling = meldinger.subList(fraIndex, tilIndex + 1)
        if (!erAktuell(måling)) return false
        måling.målingFerdig(navn(måling), logger)
        return true
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(Måling::class.java)

        private fun List<Melding>.indexOfLastOrNull(predicate: (melding: Melding) -> Boolean) =
            indexOfLast(predicate).takeUnless { it < 0 }

        private fun List<Melding>.indexOfFirstOrNull(predicate: (melding: Melding) -> Boolean) =
            indexOfFirst(predicate).takeUnless { it < 0 }

        private val rapidMeasurement = Histogram
            .build("rapid_measurement", "Måler tiden ting tar på rapiden")
            .labelNames("measurement")
            .register()

        internal fun List<Melding>.målingFerdig(navn: String, log: Logger) {
            val totaltid = totaltid()
            rapidMeasurement.labels(navn).observe(totaltid.toSeconds().toDouble())
            log.info("Måling $navn tok ${totaltid.formaterDuration()} ${formater()}")
        }
    }
}