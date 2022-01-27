package no.nav.helse.spotter.event

import java.time.LocalDateTime
import java.util.*

internal abstract class Event(
    private val id: UUID,
    private val navn: String,
    private val opprettet: LocalDateTime,
    private val forårsaketAv: Event? = null
) {
    internal fun erForårsaketAv(event: Event) = forårsaketAv?.id == event.id
    internal abstract fun erRelevantMed(event: Event) : Boolean
    internal abstract fun erRelevantMed(event: VedtaksperiodeEndret) : Boolean
}