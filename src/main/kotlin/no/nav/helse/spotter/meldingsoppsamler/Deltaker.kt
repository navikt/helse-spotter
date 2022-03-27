package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.opprettet
import java.time.LocalDateTime

internal class Deltaker(
    internal val tidspunkt: LocalDateTime,
    internal val navn: String) {
    override fun toString() = "$navn @ $tidspunkt"
    override fun equals(other: Any?) = other is Deltaker && this.tidspunkt == tidspunkt && this.navn == navn
    override fun hashCode() = tidspunkt.hashCode() + navn.hashCode()

    companion object {
        internal fun JsonNode.resolveDeltaker() : Deltaker {
            val deltakere = get("system_participating_services") ?: return Deltaker(tidspunkt = opprettet, navn = "Ukjent")
            return deltakere.map { Deltaker(tidspunkt = LocalDateTime.parse(it["time"].asText()), navn = it.get("service").asText()) }.maxByOrNull { it.tidspunkt }!!
        }

        internal fun JsonNode.antallDeltakere() = get("system_participating_services")?.size() ?: 0
    }
}