package no.nav.helse.spotter.meldingsoppsamler

internal interface MeldingsgruppeListener {
    fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>)
}