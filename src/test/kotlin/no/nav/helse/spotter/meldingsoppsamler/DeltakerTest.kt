package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotter.meldingsoppsamler.Deltaker.Companion.antallDeltakere
import no.nav.helse.spotter.meldingsoppsamler.Deltaker.Companion.resolveDeltaker
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class DeltakerTest {

    @Test
    fun `uten participating services`() {
        val tidspunkt = "2022-03-27T12:14:09.639191525"
        @Language("JSON")
        val json = """{"@opprettet": "$tidspunkt"}"""
        assertEquals(Deltaker(navn = "Ukjent", tidspunkt = LocalDateTime.parse(tidspunkt)), jacksonObjectMapper().readTree(json).resolveDeltaker())
        assertEquals(0, jacksonObjectMapper().readTree(json).antallDeltakere())
    }

    @Test
    fun `med participating services`() {
        val tidspunkt = "2022-03-27T12:20:09.708111042"
        @Language("JSON")
        val json = """
        {
            "system_participating_services": [
                {
                  "service": "spleis",
                  "time": "2022-03-27T12:14:09.639191525"
                },
                {
                  "service": "sparkel-personinfo",
                  "time": "2022-03-27T12:14:09.670554421"
                },
                {
                  "service": "behovsakkumulator",
                  "time": "$tidspunkt"
                },
                {
                  "service": "spleis",
                  "time": "2022-03-27T12:14:10.073270734"
                }
            ]
        }
        """.trimIndent()
        assertEquals(Deltaker(navn = "behovsakkumulator", tidspunkt = LocalDateTime.parse(tidspunkt)), jacksonObjectMapper().readTree(json).resolveDeltaker())
        assertEquals(4, jacksonObjectMapper().readTree(json).antallDeltakere())
    }
}