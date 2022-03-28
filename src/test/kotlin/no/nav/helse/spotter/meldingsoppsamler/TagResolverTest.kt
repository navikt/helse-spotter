package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spotter.meldingsoppsamler.TagResolver.resolveTags
import no.nav.helse.spotter.meldingsoppsamler.TagResolver.typedTag
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class TagResolverTest {

    @Test
    fun `alle tags i meldingen`() {
        @Language("JSON")
        val json = """
           {
              "system_participating_services": [{"name": "test", "time": "${LocalDateTime.now()}"}],
              "test": "EN_ARBEIDSGIVER",
              "FLERE_ARBEIDSGIVERE": 1,
              "@final": "mjau",
              "@behov": ["A", "b", "c"]
           } 
        """

        val forventet = mapOf(
            "antallDeltakere" to 1,
            "enArbeidsgiver" to true,
            "flereArbeidsgivere" to true,
            "@final" to true,
            "etterspurteBehov" to listOf("A", "b", "c")
        )
        assertEquals(forventet, jacksonObjectMapper().readTree(json).resolveTags())
        assertEquals(1, forventet.typedTag("antallDeltakere"))
        assertEquals(true, forventet.typedTag("enArbeidsgiver"))
        assertEquals(true, forventet.typedTag("flereArbeidsgivere"))
        assertEquals(true, forventet.typedTag("@final"))
        assertEquals(listOf("A", "b", "c"), forventet.typedTag("etterspurteBehov"))
    }

    @Test
    fun `ingen tags i meldingen`() {
        val forventet = mapOf(
            "antallDeltakere" to 0,
            "enArbeidsgiver" to false,
            "flereArbeidsgivere" to false,
            "@final" to false,
            "etterspurteBehov" to emptyList<String>()
        )
        assertEquals(forventet, jacksonObjectMapper().readTree("{}").resolveTags())
        assertEquals(0, forventet.typedTag("antallDeltakere"))
        assertEquals(false, forventet.typedTag("enArbeidsgiver"))
        assertEquals(false, forventet.typedTag("flereArbeidsgivere"))
        assertEquals(false, forventet.typedTag("@final"))
        assertEquals(emptyList<String>(), forventet.typedTag("etterspurteBehov"))
    }
}