package no.nav.helse.spotter.måling

import no.nav.helse.spotter.event.Event
import no.nav.helse.spotter.event.VedtaksperiodeEndret
import java.time.LocalDateTime
import java.util.*

internal class OverstyrInntektMåling(
    id: UUID,
    opprettet: LocalDateTime
): Måling, Event(id, "overstyr_inntekt", opprettet) {
    private var state: State = State.MålingStartet
    private val events = mutableListOf<Event>()

    override fun håndter(event: VedtaksperiodeEndret) {
        state.håndter(this, event)
    }

    override fun ferdig(): Resultat {
        TODO("Not yet implemented")
    }

    override fun state(): Måling.State {
        TODO("Not yet implemented")
    }

    private interface State {
        fun håndter(måling: OverstyrInntektMåling, event: VedtaksperiodeEndret) {}
        object MålingStartet : State {
            override fun håndter(måling: OverstyrInntektMåling, event: VedtaksperiodeEndret) {
                if (!event.erForårsaketAv(måling)) return
                måling.events.add(event)
                måling.state = AvventerNoe
            }
        }
        object AvventerNoe: State {
            override fun håndter(måling: OverstyrInntektMåling, event: VedtaksperiodeEndret) {
                if (!måling.erRelevantMed(event)) return
                //if (!event.erForårsaketAv(måling) && måling.events.none { it.erRelevantMed(event) }) return
                måling.events.add(event)
            }
        }
    }

    override fun erRelevantMed(event: Event): Boolean {
        return event.erForårsaketAv(this) || events.any { it.erRelevantMed(event) }
    }

    override fun erRelevantMed(event: VedtaksperiodeEndret): Boolean {
        TODO("Not yet implemented")
    }

}