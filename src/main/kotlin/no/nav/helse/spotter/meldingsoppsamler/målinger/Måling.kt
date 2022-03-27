package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.MeldingsgruppeListener
import no.nav.helse.spotter.meldingsoppsamler.Melding
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

internal abstract class Måling(
    private val fra: String,
    private val til: String,
    private val erAktuell: (måling: List<Melding>) -> Boolean = { true }
) : MeldingsgruppeListener {

    override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>) {
        if (nyMelding.navn != til) return
        val fraIndex = meldinger.indexOfLastOrNull { it.navn == fra } ?: return
        val tilIndex = meldinger.indexOfLastOrNull { it.navn == til } ?: return
        if (fraIndex <= tilIndex) return
        val måling = meldinger.subList(fraIndex, tilIndex + 1)
        if (!erAktuell(måling)) return
        logger.info(måling.formater())
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(Måling::class.java)

        private fun List<Melding>.indexOfLastOrNull(predicate: (melding: Melding) -> Boolean): Int? {
            val index = indexOfLast(predicate)
            if (index < 0) return null
            return index
        }

        private fun Duration.formater() = "${toSeconds()} sekunder & ${toMillisPart()} millisekunder"

        internal fun List<Melding>.formater() : String {
            val header = "Måling fra ${first().navn} til ${last().navn} tok ${Duration.between(first().tidspunkt, last().tidspunkt).formater()}"
            var forrigeTidspunkt: LocalDateTime? = null
            return "$header\n-> " + joinToString("\n-> ") { melding -> when (forrigeTidspunkt) {
                null -> "${melding.navn.padEnd(30, ' ')}${melding.id}\t${melding.tidspunkt}"
                else -> "${melding.navn.padEnd(30, ' ')}${melding.id}\t${Duration.between(forrigeTidspunkt, melding.tidspunkt).formater()}"
            }.also { forrigeTidspunkt = melding.tidspunkt }}
        }
    }
}