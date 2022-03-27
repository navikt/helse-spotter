package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.Melding
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

internal abstract class Måling(
    private val navn : String,
    private val fra: (melding: Melding) -> Boolean,
    private val til: (melding: Melding) -> Boolean,
    private val erAktuell: (måling: List<Melding>) -> Boolean = { true }
) : MeldingsgruppeListener {

    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>) {
        if (!til(nyMelding)) return
        val fraIndex = meldinger.indexOfLastOrNull(fra) ?: return
        val tilIndex = meldinger.indexOfLastOrNull(til) ?: return
        if (fraIndex <= tilIndex) return
        val måling = meldinger.subList(fraIndex, tilIndex + 1)
        if (!erAktuell(måling)) return
        logger.info(måling.formater(navn))
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(Måling::class.java)

        private fun List<Melding>.indexOfLastOrNull(predicate: (melding: Melding) -> Boolean): Int? {
            val index = indexOfLast(predicate)
            if (index < 0) return null
            return index
        }

        private fun String.pad() = padEnd(35, ' ')
        private fun Duration.formater() = "${toSeconds()} sekunder & ${toMillisPart()} millisekunder".pad()
        private fun LocalDateTime.formater() = "$this".pad()

        internal fun List<Melding>.formater(navn: String) : String {
            val header = "Måling $navn tok ${Duration.between(first().deltaker.tidspunkt, last().deltaker.tidspunkt).formater()}"
            var forrigeTidspunkt: LocalDateTime? = null
            return "$header\n-> " + joinToString("\n-> ") { melding -> when (forrigeTidspunkt) {
                null -> "${melding.navn.pad()}${melding.id}\t${melding.deltaker.tidspunkt.formater()}${melding.deltaker.navn}"
                else -> "${melding.navn.pad()}${melding.id}\t${Duration.between(forrigeTidspunkt, melding.deltaker.tidspunkt).formater()}${melding.deltaker.navn}"
            }.also { forrigeTidspunkt = melding.deltaker.tidspunkt }}
        }
    }
}