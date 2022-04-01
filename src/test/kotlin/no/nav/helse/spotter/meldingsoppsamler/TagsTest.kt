package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotter.meldingsoppsamler.Tags.avventerArbeidgivereTilAvventerHistorikk
import no.nav.helse.spotter.meldingsoppsamler.Tags.gjelderEnArbeidsgiver
import no.nav.helse.spotter.meldingsoppsamler.Tags.gjelderFlereArbeidsgivere
import no.nav.helse.spotter.meldingsoppsamler.Tags.tags
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class TagsTest {

    @Test
    fun `godkjenningsbehov en arbeidsgiver`() {
        @Language("JSON")
        val json = """
           {
              "@event_name": "behov",
              "Godkjenning": {
                "inntektskilde": "EN_ARBEIDSGIVER"
              }
           } 
        """

        val forventet = mapOf(
            "behov.Godkjenning.innektskilde" to "EN_ARBEIDSGIVER"
        )

        assertEquals(forventet, json.tags())
        assertTrue(forventet.gjelderEnArbeidsgiver())
        assertFalse(forventet.gjelderFlereArbeidsgivere())
        assertFalse(forventet.avventerArbeidgivereTilAvventerHistorikk())
    }

    @Test
    fun `godkjennignsbehov flere arbeidsgivere`() {
        @Language("JSON")
        val json = """
           {
              "@event_name": "behov",
              "Godkjenning": {
                "inntektskilde": "FLERE_ARBEIDSGIVERE"
              }
           } 
        """

        val forventet = mapOf(
            "behov.Godkjenning.innektskilde" to "FLERE_ARBEIDSGIVERE"
        )

        assertEquals(forventet, json.tags())
        assertFalse(forventet.gjelderEnArbeidsgiver())
        assertTrue(forventet.gjelderFlereArbeidsgivere())
        assertFalse(forventet.avventerArbeidgivereTilAvventerHistorikk())
    }

    @Test
    fun `annet type behov`() {
        @Language("JSON")
        val json = """
           {
              "@event_name": "behov",
              "Foreldrepenger": {}
           } 
        """

        val forventet = mapOf<String, Any>()
        assertEquals(forventet, json.tags())
        assertFalse(forventet.gjelderEnArbeidsgiver())
        assertFalse(forventet.gjelderFlereArbeidsgivere())
        assertFalse(forventet.avventerArbeidgivereTilAvventerHistorikk())
    }

    @Test
    fun `vedtaksperiode_endret avventer arbeidsgivere til avventer historikk`() {
        @Language("JSON")
        val json = """
           {
              "@event_name": "vedtaksperiode_endret",
              "gjeldendeTilstand": "AVVENTER_HISTORIKK",
              "forrigeTilstand": "AVVENTER_ARBEIDSGIVERE"
           } 
        """

        val forventet = mapOf(
            "vedtaksperiode_endret.forrigeTilstand" to "AVVENTER_ARBEIDSGIVERE",
            "vedtaksperiode_endret.gjeldendeTilstand" to "AVVENTER_HISTORIKK"
        )

        assertEquals(forventet, json.tags())
        assertFalse(forventet.gjelderEnArbeidsgiver())
        assertFalse(forventet.gjelderFlereArbeidsgivere())
        assertTrue(forventet.avventerArbeidgivereTilAvventerHistorikk())
    }

    @Test
    fun `vedtaksperiode_endret annen tilstandsendring`() {
        @Language("JSON")
        val json = """
           {
              "@event_name": "vedtaksperiode_endret",
              "gjeldendeTilstand": "TIL_INFOTRYGD",
              "forrigeTilstand": "AVVENTER_ARBEIDSGIVERE"
           } 
        """

        val forventet = mapOf(
            "vedtaksperiode_endret.forrigeTilstand" to "AVVENTER_ARBEIDSGIVERE",
            "vedtaksperiode_endret.gjeldendeTilstand" to "TIL_INFOTRYGD"
        )

        assertEquals(forventet, json.tags())
        assertFalse(forventet.gjelderEnArbeidsgiver())
        assertFalse(forventet.gjelderFlereArbeidsgivere())
        assertFalse(forventet.avventerArbeidgivereTilAvventerHistorikk())
    }

    @Test
    fun `ingen tags i meldingen`() {
        val forventet = mapOf<String, Any>()
        assertEquals(forventet, "{}".tags())
        assertEquals(false, forventet.gjelderEnArbeidsgiver())
        assertEquals(false, forventet.gjelderFlereArbeidsgivere())
        assertEquals(false, forventet.avventerArbeidgivereTilAvventerHistorikk())
    }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
        fun String.tags() = objectMapper.readTree(this).tags()
    }
}