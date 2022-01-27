package no.nav.helse.spotter.event

import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeEndret(
    id: UUID,
    navn: String,
    opprettet: LocalDateTime,
    forårsaketAv: Event,
    private val vedtaksperiodeId: UUID,
) : Event(id, navn, opprettet, forårsaketAv) {
    override fun erRelevantMed(event: Event): Boolean {
        return event.erRelevantMed(this)
    }

    override fun erRelevantMed(event: VedtaksperiodeEndret): Boolean {
        return this.vedtaksperiodeId == event.vedtaksperiodeId
    }

}