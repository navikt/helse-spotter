package no.nav.helse.spotter.måling

import no.nav.helse.spotter.event.VedtaksperiodeEndret
import java.time.Duration
import java.time.LocalDateTime

internal data class Resultat(
    val startet: LocalDateTime,
    val ferdig: LocalDateTime
) {
    val tidsbruk = Duration.between(startet, ferdig)
}

internal interface Måling {
    fun håndter(event: VedtaksperiodeEndret) {}
    fun ferdig(): Resultat
    fun state(): State

    interface State {
        object Initiell: State
        object MålingStartet: State
        object MålingFerdig: State
    }
}