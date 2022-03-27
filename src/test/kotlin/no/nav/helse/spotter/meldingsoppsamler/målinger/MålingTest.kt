package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.Deltaker
import no.nav.helse.spotter.meldingsoppsamler.Melding
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class MålingTest {

    @Test
    fun `måling fra en konkret melding til en annen`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        val testMåling = object : Måling(
            navn = "testmåling",
            fra = { it.id == id1 },
            til = { it.id == id2 }
        ){}

        val melding1 = Melding(
            id = id1,
            navn = "melding",
            deltaker = Deltaker(tidspunkt = LocalDateTime.now(), navn = "test"),
            payload = ""
        )
        val melding2 = Melding(
            id = id2,
            navn = "melding2",
            deltaker = Deltaker(tidspunkt = LocalDateTime.now(), navn = "test"),
            payload = ""
        )

        assertTrue(testMåling.onNyMelding(melding2, listOf(melding1, melding2)))
    }

    @Test
    fun `måling fra og til samme melding`() {

        val testMåling = object : Måling(
            navn = "testmåling",
            fra = { true },
            til = { true }
        ){}

        val melding = Melding(
            id = UUID.randomUUID(),
            navn = "melding",
            deltaker = Deltaker(tidspunkt = LocalDateTime.now(), navn = "test"),
            payload = ""
        )


        assertTrue(testMåling.onNyMelding(melding, listOf(melding)))
    }
}