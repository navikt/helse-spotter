package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal class MvpGodkjenning(
    private val målingFerdig: (målingsresultat: Målingsresultat) -> Unit = {
        logger.info(it.toString())
        treghetHistogram.labels("Godkjenning").observe(Duration.between(it.startet, it.ferdig).toSeconds().toDouble())
    }
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MvpGodkjenning::class.java)
    }

    private val målinger = mutableListOf<Måling>()

    private fun oppdaterBasertPåVedtaksperiodeId(vedtaksperiodeId: UUID, oppdatert:(måling: Måling) -> Måling) =
        målinger.indexOfFirst {
            it.kjennerTilVedtaksperiodeId(vedtaksperiodeId)
        }.takeIf { it >= 0 }?.also { index ->
            målinger[index] = oppdatert(målinger[index])
        }?.let { målinger[it] }

    private fun oppdatertBasertPåEventId(eventId: UUID, oppdatert:(måling: Måling) -> Måling) =
        målinger.indexOfFirst {
            it.kjennerTilEventId(eventId)
        }.takeIf { it >= 0 }?.also { index ->
            målinger[index] = oppdatert(målinger[index])
        }?.let { målinger[it] }

    private fun håndterGodkjenningsbehov(behov: Godkjenningsbehov) {
        if (oppdaterBasertPåVedtaksperiodeId(behov.vedtaksperiodeId) { it.leggTil(behov) } != null) return
        else målinger.add(Måling(behov))
    }

    private fun håndterVedtaksperiodeEndret(behov: VedtaksperiodeEndret) {
        if (oppdaterBasertPåVedtaksperiodeId(behov.vedtaksperiodeId) { it.leggTil(behov) } != null) return
        oppdatertBasertPåEventId(behov.forårsaketAv) { it.leggTil(behov) }
    }

    private fun håndterSaksbehandlerløsning(behov: Saksbehandlerløsning) {
        oppdatertBasertPåEventId(behov.hendelseId) { it.leggTil(behov)}
    }

    private fun håndterOppgaveOpprettet(behov: OppgaveOpprettet) =
        oppdatertBasertPåEventId(behov.hendelseId) { it.leggTil(behov)}

    internal fun registrer(rapidsCliApplication: RapidsCliApplication) {
        rapidsCliApplication.apply {
            JsonRiver(this).apply {
                validate(harStandardfelter())
                validate(onlyEvents("behov"))
                validate { _, node, _ -> "Godkjenning" in node.path("@behov").map(JsonNode::asText) }
                validate { _, node, _ -> !node.hasNonNull("@løsning") } // bare uløste behov
                validate(textFieldValidation("vedtaksperiodeId"))
                validate { _, node, _ -> node.path("Godkjenning").path("inntektskilde").asText() == "FLERE_ARBEIDSGIVERE" }
                validate { _, node, _ -> node.path("Godkjenning").path("aktiveVedtaksperioder").isArray }
                onMessage { _, node ->
                    håndterGodkjenningsbehov(Godkjenningsbehov(node))
                }
            }
            JsonRiver(this).apply {
                validate(harStandardfelter())
                validate(onlyEvents("vedtaksperiode_endret"))
                validate(textFieldValidation("vedtaksperiodeId"))
                validate(textFieldValidation("gjeldendeTilstand"))
                validate(textFieldValidation("forrigeTilstand"))
                onMessage { _, node ->
                    håndterVedtaksperiodeEndret(VedtaksperiodeEndret(node))
                }
            }
            JsonRiver(this).apply {
                validate(harStandardfelter())
                validate(onlyEvents("saksbehandler_løsning"))
                validate(textFieldValidation("hendelseId"))
                onMessage { _, node ->
                    håndterSaksbehandlerløsning(Saksbehandlerløsning(node))
                }
            }
            JsonRiver(this).apply {
                validate(harStandardfelter())
                validate(onlyEvents("oppgave_opprettet"))
                validate(textFieldValidation("hendelseId"))
                onMessage { _, node ->
                    håndterOppgaveOpprettet(OppgaveOpprettet(node))?.also { måling ->
                        måling.måling()?.also { målingsresultat ->
                            målingFerdig(målingsresultat)
                            målinger.remove(måling)
                        }
                    }
                }
            }
        }
    }

    data class Målingsresultat(
        val events: List<Event>,
        val startet: LocalDateTime,
        val ferdig: LocalDateTime) {
        override fun toString() = "Måling for godkjenning: tok totalt ${Duration.between(startet, ferdig).formater()}\n-> " +
            events.joinToString("\n-> ") {
                if (it.opprettet < startet) "${it.navn} - Før måling startet ($it)"
                else "${it.navn} - ${Duration.between(startet, it.opprettet).formater()} ($it)"
            }
    }

    private class Måling(behov: Godkjenningsbehov) {
        private val godkjenningsbehov = mutableListOf(behov)
        private val vedtaksperiodeEndret = mutableListOf<VedtaksperiodeEndret>()
        private val saksbehandlerløsninger = mutableListOf<Saksbehandlerløsning>()
        private val oppgaveOpprettet = mutableListOf<OppgaveOpprettet>()
        private val events get() = godkjenningsbehov + vedtaksperiodeEndret + saksbehandlerløsninger + oppgaveOpprettet

        fun leggTil(vedtaksperiodeEndret: VedtaksperiodeEndret) : Måling {
            this.vedtaksperiodeEndret.add(vedtaksperiodeEndret)
            return this
        }
        fun leggTil(godkjenningsbehov: Godkjenningsbehov) : Måling {
            this.godkjenningsbehov.add(godkjenningsbehov)
            return this
        }
        fun leggTil(saksbehandlerløsning: Saksbehandlerløsning) : Måling {
            saksbehandlerløsninger.add(saksbehandlerløsning)
            return this
        }
        fun leggTil(oppgaveOpprettet: OppgaveOpprettet) : Måling {
            this.oppgaveOpprettet.add(oppgaveOpprettet)
            return this
        }

        fun kjennerTilVedtaksperiodeId(vedtaksperiodeId: UUID) =
            godkjenningsbehov.any { it.vedtaksperiodeId == vedtaksperiodeId } ||
            godkjenningsbehov.any { it.aktiveVedtaksperioder.contains(vedtaksperiodeId) } ||
            vedtaksperiodeEndret.any { it.vedtaksperiodeId == vedtaksperiodeId }

        fun kjennerTilEventId(id: UUID) =
            events.any { it.id == id } ||
            saksbehandlerløsninger.any { it.hendelseId == it.id } ||
            oppgaveOpprettet.any { it.hendelseId == it.id }

        fun måling() = events.firstOrNull { it.navn == "saksbehandler_løsning" }?.let { saksbehadlerløsning ->
            Målingsresultat(
                events = events.sortedBy { it.opprettet },
                startet = saksbehadlerløsning.opprettet,
                ferdig = events.last().opprettet
            )
        }
    }

    private class Godkjenningsbehov(
        node: JsonNode
    ) : Event(node.id, node.opprettet, "@behov.Godkjenning") {
        val vedtaksperiodeId = node.vedtaksperiodeId
        val aktiveVedtaksperioder = node["Godkjenning"].path("aktiveVedtaksperioder").map { UUID.fromString(it.asText()) }.toSet()
        init {
            info["vedtaksperiodeId"] = vedtaksperiodeId
            info["aktiveVedtaksperioder"] = aktiveVedtaksperioder
        }
    }

    private class VedtaksperiodeEndret(
        node: JsonNode
    ) : Event(node.id, node.opprettet, node.eventName) {
        val vedtaksperiodeId = node.vedtaksperiodeId
        val forårsaketAv = node.forårsaketAv
        val gjeldendeTilstand = node.path("gjeldendeTilstand").asText()
        val forrigeTilstand = node.path("forrigeTilstand").asText()
        init {
            info["vedtaksperiodeId"] = vedtaksperiodeId
            info["forårsaketAv"] = forårsaketAv
            info["gjeldendeTilstand"] = gjeldendeTilstand
            info["forrigeTilstand"] = forrigeTilstand
        }
    }

    private class Saksbehandlerløsning(
        node: JsonNode
    ) : Event(node.id, node.opprettet, node.eventName) {
        val hendelseId = UUID.fromString(node.path("hendelseId").asText())
        init {
            info["hendelseId"] = hendelseId
        }
    }

    private class OppgaveOpprettet(
        node: JsonNode
    ) : Event(node.id, node.opprettet, node.eventName) {
        val hendelseId = UUID.fromString(node.path("hendelseId").asText())
        init {
            info["hendelseId"] = hendelseId
        }
    }
}