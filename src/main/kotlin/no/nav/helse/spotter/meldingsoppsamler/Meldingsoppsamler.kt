package no.nav.helse.spotter.meldingsoppsamler

import org.slf4j.LoggerFactory

internal class Meldingsoppsamler(
    vararg listeners: MeldingsgruppeListener
) {
    private val listeners = listeners.toList()
    private val meldingsgrupper = mutableListOf<Meldingsgruppe>()

    internal fun leggTil(melding: Melding) {
        val eksisterendeKobledeMeldinger = meldingsgrupper.mapNotNull { it.leggTil(melding) }.onEach { eksisterende ->
            listeners.forEach { listener -> listener.onNyMelding(melding, eksisterende.meldinger()) }
        }
        if (eksisterendeKobledeMeldinger.isNotEmpty()) return finalize(melding)

        val ny = Meldingsgruppe(melding).also { meldingsgrupper.add(it) }
        listeners.forEach { listener -> listener.onNyMelding(melding, ny.meldinger()) }

        finalize(melding)
    }

    internal fun antallMeldingsgrupper() = meldingsgrupper.size

    private fun finalize(melding: Melding) {
        val antallFørSletting = antallMeldingsgrupper()
        val tidspunkt = melding.deltaker.tidspunkt.minusMinutes(10)
        meldingsgrupper.removeIf { !it.oppdatertEtter(tidspunkt) }
        logger.info("Inneholder nå ${antallMeldingsgrupper()} meldingsgruppe(r) etter håndtering av ${melding.navn}")
        (antallFørSletting-antallMeldingsgrupper()).takeIf { it > 0 }?.also {
            logger.info("Slettet $it meldingsgruppe(r) som ikke hadde blitt oppdatert på 10 minutter")
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Meldingsoppsamler::class.java)
    }
}