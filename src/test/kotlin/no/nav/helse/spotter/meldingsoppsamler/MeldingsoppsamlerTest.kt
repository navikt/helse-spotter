package no.nav.helse.spotter.meldingsoppsamler

import no.nav.helse.spotter.meldingsoppsamler.målinger.Måling.Companion.formater
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

internal class MeldingsoppsamlerTest {

    private companion object {
        private var nå = now()
        fun nesteDeltaker() = nå.deltaker().also {
            nå = it.tidspunkt.plusSeconds(30).plusNanos(500000000)
        }
        fun LocalDateTime.deltaker() = Deltaker(navn = "test", tidspunkt = this)
    }

    private val testListener = object : MeldingsgruppeListener {
        var sisteKobledeMeldinger: List<Melding> = emptyList()
        override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>) {
            println(meldinger.formater("Testmåling"))
            this.sisteKobledeMeldinger = meldinger
        }
    }

    @Test
    fun `kobler sammen riktige meldinger`() {
        val meldingsoppsamler = Meldingsoppsamler(testListener)

        val id1 = UUID.randomUUID()
        val melding1 = Melding(id1,"melding1", nesteDeltaker(), "")
        meldingsoppsamler.leggTil(melding1)
        assertEquals(listOf(melding1), testListener.sisteKobledeMeldinger)

        val id2 = UUID.randomUUID()
        val melding2 = Melding(id2, "melding2", nesteDeltaker(), "$id1")
        meldingsoppsamler.leggTil(melding2)
        assertEquals(listOf(melding1, melding2), testListener.sisteKobledeMeldinger)

        val id3 = UUID.randomUUID()
        val melding3 = Melding(id3, "melding3", nesteDeltaker(), "")
        meldingsoppsamler.leggTil(melding3)
        assertEquals(listOf(melding3), testListener.sisteKobledeMeldinger)

        val id4 = UUID.randomUUID()
        val melding4 = Melding(id4, "melding4", nesteDeltaker(), "$id2")
        meldingsoppsamler.leggTil(melding4)
        assertEquals(listOf(melding1, melding2, melding4), testListener.sisteKobledeMeldinger)
    }

    @Test
    fun `sletter meldingsgrupper som ikke har vært oppdatert på ti minutter`() {
        val meldingsoppsamler = Meldingsoppsamler(testListener)
        val now = now()

        val id1 = UUID.randomUUID()
        val skalIkkeSlettes = Melding(id1,"skalIkkeSlettes", now.minusMinutes(9).plusSeconds(59).deltaker(), "")
        meldingsoppsamler.leggTil(skalIkkeSlettes)
        assertEquals(listOf(skalIkkeSlettes), testListener.sisteKobledeMeldinger)
        assertEquals(1, meldingsoppsamler.antallMeldingsgrupper())

        val id2 = UUID.randomUUID()
        val skalSlettes = Melding(id2,"skalSlettes", now.minusHours(10).deltaker(), "")
        meldingsoppsamler.leggTil(skalSlettes)
        assertEquals(listOf(skalSlettes), testListener.sisteKobledeMeldinger)
        assertEquals(2, meldingsoppsamler.antallMeldingsgrupper())

        val id3 = UUID.randomUUID()
        val nyMelding = Melding(id3, "nyMelding", now.deltaker(), "")
        meldingsoppsamler.leggTil(nyMelding)
        assertEquals(listOf(nyMelding), testListener.sisteKobledeMeldinger)
        assertEquals(2, meldingsoppsamler.antallMeldingsgrupper())
    }

    @Test
    fun `melding med samme id legges til flere ganger i samme gruppe`() {
        val meldingsoppsamler = Meldingsoppsamler(testListener)

        val id1 = UUID.randomUUID()
        val melding1 = Melding(id1,"melding1", nesteDeltaker(), "")
        val melding2 = Melding(id1,"melding1", nesteDeltaker(), "")
        meldingsoppsamler.leggTil(melding1)
        meldingsoppsamler.leggTil(melding2)
        assertEquals(listOf(melding1, melding2), testListener.sisteKobledeMeldinger)

    }
}