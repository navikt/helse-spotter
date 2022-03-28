package no.nav.helse.spotter.meldingsoppsamler

import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formater
import no.nav.helse.spotter.meldingsoppsamler.Melding.Companion.formatertTotaltid
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

        private val testListener = object : MeldingsgruppeListener {
            var sisteMeldingsgruppe: List<Melding> = emptyList()
            override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>) : Boolean {
                println("Siste meldingsgruppe ${meldinger.formatertTotaltid()} ${meldinger.formater()}")
                this.sisteMeldingsgruppe = meldinger
                return true
            }
        }
        fun nyMeldingsoppsamler() = Meldingsoppsamler(
            timeoutListener = object : MeldingsgruppeListener {
                override fun onNyMelding(nyMelding: Melding, meldinger: List<Melding>) : Boolean {
                    println("Sletter meldingsgruppe ${meldinger.formatertTotaltid()} ${meldinger.formater()}")
                    return true
                }
            },
            listeners = listOf(testListener),
            rydde = { true }
        )
    }

    @Test
    fun `kobler sammen riktige meldinger`() {
        val meldingsoppsamler = nyMeldingsoppsamler()

        val id1 = UUID.randomUUID()
        val melding1 = TestMelding(id1, "melding1", nesteDeltaker()).melding
        meldingsoppsamler.leggTil(melding1)
        assertEquals(listOf(melding1), testListener.sisteMeldingsgruppe)

        val id2 = UUID.randomUUID()
        val melding2 = TestMelding(id2, "melding2", nesteDeltaker(), setOf(id1)).melding
        meldingsoppsamler.leggTil(melding2)
        assertEquals(listOf(melding1, melding2), testListener.sisteMeldingsgruppe)

        val id3 = UUID.randomUUID()
        val melding3 = TestMelding(id3, "melding3", nesteDeltaker()).melding
        meldingsoppsamler.leggTil(melding3)
        assertEquals(listOf(melding3), testListener.sisteMeldingsgruppe)

        val id4 = UUID.randomUUID()
        val melding4 = TestMelding(id4, "melding4", nesteDeltaker(), setOf(id2)).melding
        meldingsoppsamler.leggTil(melding4)
        assertEquals(listOf(melding1, melding2, melding4), testListener.sisteMeldingsgruppe)
    }

    @Test
    fun `sletter meldingsgrupper som ikke har vært oppdatert på ti minutter`() {
        val meldingsoppsamler = nyMeldingsoppsamler()
        val now = now()

        val id1 = UUID.randomUUID()
        val skalIkkeSlettes = TestMelding(id1,"skalIkkeSlettes", now.minusMinutes(9).plusSeconds(59).deltaker()).melding
        meldingsoppsamler.leggTil(skalIkkeSlettes)
        assertEquals(listOf(skalIkkeSlettes), testListener.sisteMeldingsgruppe)
        assertEquals(1, meldingsoppsamler.antallMeldingsgrupper())

        val id2 = UUID.randomUUID()
        val skalSlettes = TestMelding(id2,"skalSlettes", now.minusHours(10).deltaker()).melding
        meldingsoppsamler.leggTil(skalSlettes)
        assertEquals(listOf(skalSlettes), testListener.sisteMeldingsgruppe)
        assertEquals(2, meldingsoppsamler.antallMeldingsgrupper())

        val id3 = UUID.randomUUID()
        val nyMelding = TestMelding(id3, "nyMelding", now.deltaker()).melding
        meldingsoppsamler.leggTil(nyMelding)
        assertEquals(listOf(nyMelding), testListener.sisteMeldingsgruppe)
        assertEquals(2, meldingsoppsamler.antallMeldingsgrupper())
    }

    @Test
    fun `melding med samme id legges til flere ganger i samme gruppe`() {
        val meldingsoppsamler = nyMeldingsoppsamler()

        val id1 = UUID.randomUUID()
        val melding1 = TestMelding(id1,"melding1", nesteDeltaker()).melding
        val melding2 = TestMelding(id1,"melding1", nesteDeltaker()).melding
        meldingsoppsamler.leggTil(melding1)
        meldingsoppsamler.leggTil(melding2)
        assertEquals(listOf(melding1, melding2), testListener.sisteMeldingsgruppe)
    }
}