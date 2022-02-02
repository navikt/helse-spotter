package no.nav.helse.spotter.måling

import no.nav.helse.spotter.MvpGodkjenningFlereArbeidsgivere
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class MvpGodkjenningFlereArbeidsgivereTest {

    private val målinger = mutableListOf<MvpGodkjenningFlereArbeidsgivere.Målingsresultat>()
    private val testRapidsCliApplication = TestRapidsCliApplication()
    private val måling = MvpGodkjenningFlereArbeidsgivere { måling -> målinger.add(måling) }.also {
        it.registrer(testRapidsCliApplication.rapidsCliApplication)
    }

    @BeforeEach
    fun reset() {
        målinger.clear()
    }

    @Test
    fun `to vedtaksperioder`() {
        val godkjenningId1 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val godkjenningId2 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val koblingHendelseY = UUID.randomUUID()

        assertEquals(0, måling.antallPågåendeMålinger())
        // Måling starter når saksbehandler sender løsning for godkjenning av vedtaksperiode 1 (Spesialist)
        testRapidsCliApplication.send(
            saksbehandlerløsning(
                hendelseId = godkjenningId1
            )
        )
        assertEquals(1, måling.antallPågåendeMålinger())

        // Løsning på godkjenningsbehov på vedtaksperiode 1 (Spesialist)
        testRapidsCliApplication.send(
            godkjenningsbehov(
                medLøsning = true,
                id = godkjenningId1,
                vedtaksperiodeId = vedtaksperiodeId1,
                aktiveVedtaksperioder = setOf(vedtaksperiodeId2)
            )
        )

        // Vedtaksperiode 1 går til Avsluttet i Spleis
        testRapidsCliApplication.send(
            vedtaksperiodeEndret(
                vedtaksperiodeId = vedtaksperiodeId1,
                forårsaketAv = koblingHendelseY,
                gjeldendeTilstand = "AVSLUTTET"
            )
        )

        // Vedtaksperiode 2 går fra AVVENTER_ARBEIDSGIVERE i Spleis
        testRapidsCliApplication.send(
            vedtaksperiodeEndret(
                vedtaksperiodeId = vedtaksperiodeId2,
                forårsaketAv = koblingHendelseY,
                forrigeTilstand = "AVVENTER_ARBEIDSGIVERE"
            )
        )

        // Spleis sender ut godkjenningsbehov på vedtaksperiode 2
        testRapidsCliApplication.send(
            godkjenningsbehov(
                medLøsning = false,
                id = godkjenningId2,
                vedtaksperiodeId = vedtaksperiodeId2,
                aktiveVedtaksperioder = emptySet()
            )
        )

        assertEquals(1, måling.antallPågåendeMålinger())
        assertEquals(0, målinger.size)

        // Måling ferdig når det blir opprettet oppgave på Vedtaksperiode 2
        testRapidsCliApplication.send(
            oppgaveOpprettet(godkjenningId2)
        )

        assertEquals(0, måling.antallPågåendeMålinger())
        assertEquals(1, målinger.size)
        assertEquals(
            listOf(
                "saksbehandler_løsning",
                "Godkjenningsløsning",
                "vedtaksperiode_endret",
                "vedtaksperiode_endret",
                "Godkjenningsbehov",
                "oppgave_opprettet"
            ), målinger.first().events.map { it.navn })
        println(målinger.first())
    }

    @Test
    fun `sletter måling om det er løsning uten aktive vedtaksperioder`() {
        assertEquals(0, måling.antallPågåendeMålinger())
        val godkjenningId = UUID.randomUUID()
        testRapidsCliApplication.send(
            saksbehandlerløsning(
                hendelseId = godkjenningId
            )
        )
        assertEquals(1, måling.antallPågåendeMålinger())

        testRapidsCliApplication.send(
            godkjenningsbehov(
                medLøsning = true,
                id = godkjenningId,
                vedtaksperiodeId = UUID.randomUUID(),
                aktiveVedtaksperioder = emptySet()
            )
        )
        assertEquals(0, måling.antallPågåendeMålinger())
        assertEquals(0, målinger.size)

    }

    @Language("JSON")
    private fun godkjenningsbehov(
        id: UUID,
        vedtaksperiodeId: UUID,
        aktiveVedtaksperioder: Set<UUID>,
        forårsaketAv: UUID = UUID.randomUUID(),
        medLøsning: Boolean
    ) = """
        {
          "@event_name": "behov",
          "@id": "$id",
          "@opprettet": "${LocalDateTime.now()}",
          "@forårsaket_av": {
            "id": "$forårsaketAv"
          },
          "@behov": ["Godkjenning"],
          ${when (medLøsning) {
              true -> """"@løsning": {"Godkjenning": {"foo": "bar"}},"""
              false -> ""
          }}
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "Godkjenning": {
            "aktiveVedtaksperioder": ${aktiveVedtaksperioder.map { """{"vedtaksperiodeId": "$it"}""" }},
            "inntektskilde": "FLERE_ARBEIDSGIVERE"
          }
        }
    """

    @Language("JSON")
    private fun saksbehandlerløsning(hendelseId: UUID) = """
        {
          "@event_name": "saksbehandler_løsning",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "hendelseId": "$hendelseId"
        }
    """

    @Language("JSON")
    private fun oppgaveOpprettet(hendelseId: UUID) = """
        {
          "@event_name": "oppgave_opprettet",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "hendelseId": "$hendelseId"
        }
    """

    @Language("JSON")
    private fun vedtaksperiodeEndret(
        vedtaksperiodeId: UUID,
        forårsaketAv: UUID,
        gjeldendeTilstand: String = "hva-som-helst",
        forrigeTilstand: String = "hva-som-helst"
    ) = """
        {
          "@event_name": "vedtaksperiode_endret",
          "@id": "${UUID.randomUUID()}",
          "@opprettet": "${LocalDateTime.now()}",
          "@forårsaket_av": {
            "id": "$forårsaketAv"
          },
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "gjeldendeTilstand": "$gjeldendeTilstand",
          "forrigeTilstand":  "$forrigeTilstand"
        }
    """
}