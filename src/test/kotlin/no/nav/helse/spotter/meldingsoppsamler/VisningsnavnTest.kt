package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotter.meldingsoppsamler.Visningsnavn.visningsnavn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VisningsnavnTest {

    private val objectMapper = jacksonObjectMapper()
    private val behovId = "50792165-984f-4a2a-89fb-11d5c91da9a3"

    @Test
    fun `annet enn behov`() {
        assertEquals("vedtak_fattet", objectMapper.readTree("""{"@event_name": "vedtak_fattet", "@behovId": "$behovId"}""").visningsnavn())
    }

    @Test
    fun `etterspurte ett behov`() {
        assertEquals("behov   9a3 (Test)", objectMapper.readTree("""{"@event_name": "behov", "@behov": ["Test"], "@behovId": "$behovId"}""").visningsnavn())
    }

    @Test
    fun `etterspurte flere behov`() {
        assertEquals("behov   9a3 (2 stykk)", objectMapper.readTree("""{"@event_name": "behov", "@behov": ["Test", "Test2"], "@behovId": "$behovId"}""").visningsnavn())
    }

    @Test
    fun `komplett løsning`() {
        assertEquals("løsning 9a3 (komplett)", objectMapper.readTree("""{"@event_name": "behov", "@final": true, "@behovId": "$behovId"}""").visningsnavn())
    }

    @Test
    fun `en løsning`() {
        assertEquals("løsning 9a3 (TestBehov)", objectMapper.readTree("""{"@event_name": "behov", "@løsning": {"TestBehov":{}}, "@behovId": "$behovId"}""").visningsnavn())
    }

    @Test
    fun `flere løsninger`() {
        assertEquals("løsning 9a3 (HentInfotrygdutbet..,DigitalKontaktinfo..)", objectMapper.readTree("""{"@event_name": "behov", "@løsning": {"HentInfotrygdutbetalinger":{}, "DigitalKontaktinformasjon": []}, "@behovId": "$behovId"}""").visningsnavn())
    }
}