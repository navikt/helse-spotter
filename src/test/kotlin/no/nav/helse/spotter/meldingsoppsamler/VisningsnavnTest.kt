package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotter.meldingsoppsamler.Visningsnavn.visningsnavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VisningsnavnTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `annet enn behov`() {
        assertEquals("vedtak_fattet", objectMapper.readTree("""{"@event_name": "vedtak_fattet"}""").visningsnavn())
    }

    @Test
    fun `etterspurte behov`() {
        assertEquals("behov", objectMapper.readTree("""{"@event_name": "behov"}""").visningsnavn())
    }

    @Test
    fun `komplett løsning`() {
        assertEquals("løsning (komplett)", objectMapper.readTree("""{"@event_name": "behov", "@final": true}""").visningsnavn())
    }

    @Test
    fun `en løsning`() {
        assertEquals("løsning (TestBehov)", objectMapper.readTree("""{"@event_name": "behov", "@løsning": {"TestBehov":{}}}""").visningsnavn())
    }

    @Test
    fun `flere løsninger`() {
        assertEquals("løsning (TestBehov,TestBehov2)", objectMapper.readTree("""{"@event_name": "behov", "@løsning": {"TestBehov":{}, "TestBehov2": []}}""").visningsnavn())
    }
}