package no.nav.helse.spotter.meldingsoppsamler

import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class Meldingsoppsamler(
    timeoutListener: MeldingsgruppeListener,
    listeners: List<MeldingsgruppeListener>,
    private val rydde: (tidspunkt: LocalDateTime) -> Boolean = { it.minute % 5 == 0 }
) {
    private val meldingsgruppeListeners = MeldingsgruppeListeners(
        listeners = listeners,
        timeoutListener = timeoutListener
    )

    private val meldingsgrupper = mutableListOf<Meldingsgruppe>()

    internal fun leggTil(melding: Melding) {
        val eksisterendeKobledeMeldinger = meldingsgrupper.mapNotNull { it.leggTil(melding) }.onEach { eksisterende ->
            meldingsgruppeListeners.onNyMelding(melding, eksisterende.meldinger())
        }
        if (eksisterendeKobledeMeldinger.isNotEmpty()) return finalize(melding)

        val ny = Meldingsgruppe(melding).also { meldingsgrupper.add(it) }
        meldingsgruppeListeners.onNyMelding(melding, ny.meldinger())

        finalize(melding)
    }

    internal fun antallMeldingsgrupper() = meldingsgrupper.size

    private fun finalize(melding: Melding) {
        val tidspunkt = melding.deltaker.tidspunkt.minusMinutes(10)
        if (!rydde(tidspunkt)) return
        val antallMeldingsdgrupperFørSletting = meldingsgrupper.size
        meldingsgrupper.removeIf { (!it.oppdatertEtter(tidspunkt)).also { slettes -> if (slettes) {
            meldingsgruppeListeners.onTimeout(it.meldinger())
        }}}
        (antallMeldingsdgrupperFørSletting - meldingsgrupper.size).takeIf { it > 0 }?.also {
            logger.info("Slettet $it meldingsgruppe(r) som ikke er oppdatert på 10 minutter.")
        }
    }
    private companion object {
        private val logger = LoggerFactory.getLogger(Meldingsoppsamler::class.java)
    }
}