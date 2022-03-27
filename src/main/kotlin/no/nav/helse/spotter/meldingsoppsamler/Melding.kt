package no.nav.helse.spotter.meldingsoppsamler

import java.util.UUID

internal class Melding(
    val id: UUID,
    val navn: String,
    val deltaker: Deltaker,
    val payload: String
) {
    override fun equals(other: Any?) = other is Melding && other.id == id
    override fun hashCode() = id.hashCode()
    override fun toString() = "$id"
}