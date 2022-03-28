package no.nav.helse.spotter.meldingsoppsamler

import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.absoluteValue

internal class Meldingsoppsamler(
    timeoutListener: MeldingsgruppeListener,
    listeners: List<MeldingsgruppeListener>,
    private val skalRydde: (tidspunkt: LocalDateTime, ryddetSist: LocalDateTime) -> Boolean = { tidspunkt, ryddetSist ->
        tidspunkt.minute % 5 == 0 && Duration.between(ryddetSist, tidspunkt).toMinutes().absoluteValue > 2
    }) {
    private var ryddetSist = LocalDateTime.now()
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
        val tidspunkt = melding.deltaker.tidspunkt
        if (!skalRydde(tidspunkt, ryddetSist)) return

        ryddetSist = tidspunkt
        val timeout = tidspunkt.minusMinutes(10)

        val antallMeldingsdgrupperFørSletting = meldingsgrupper.size
        meldingsgrupper.removeIf { (!it.oppdatertEtter(timeout)).also { slettes -> if (slettes) {
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