package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal class MvpGodkjenningFlereArbeidsgivere(
    private val målingFerdig: (målingsresultat: Målingsresultat) -> Unit = {
        logger.info(it.toString())
        treghetHistogram.labels("GodkjenningFlereArbeidsgivere").observe(Duration.between(it.startet, it.ferdig).toSeconds().toDouble())
    }) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MvpGodkjenningFlereArbeidsgivere::class.java)
    }

    private val målinger = mutableListOf<Måling>()
    internal fun antallPågåendeMålinger() = målinger.size

    private fun oppdatertMålingBasertPåVedtaksperiodeId(vedtaksperiodeId: UUID, oppdater: (måling: Måling) -> Unit) =
        målinger.firstOrNull { it.kjennerTilVedtaksperiodeId(vedtaksperiodeId) }?.also { måling ->
            oppdater(måling)
        }

    private fun oppdaterMålingBasertPåEventId(eventId: UUID, oppdater: (måling: Måling) -> Unit) =
        målinger.firstOrNull { it.kjennerTilEventId(eventId) }?.also { måling ->
            oppdater(måling)
        }

    private fun håndterGodkjenningsbehov(behov: Godkjenningsbehov) {
        when {
            oppdatertMålingBasertPåVedtaksperiodeId(behov.vedtaksperiodeId) {
                logger.info("Utfyller eksisterende måling ${it.målingId} med Behov=$behov (basert på vedtaksperiodeId)")
                it.leggTil(behov)
            } != null -> return
            oppdaterMålingBasertPåEventId(behov.forårsaketAv) {
                logger.info("Utfyller eksisterende måling ${it.målingId} med Behov=$behov (basert på forårsaketAv)")
                it.leggTil(behov)
            } != null -> return
            // Starter kun nye målinger der hvor det finnes andre aktive Vedtaksperioder
            behov.aktiveVedtaksperioder.isNotEmpty() -> {
                val nyMåling = Måling(behov)
                logger.info("Starter ny måling ${nyMåling.målingId} fra Behov=$behov")
                målinger.add(Måling(behov))
            }
        }
    }

    private fun håndterVedtaksperiodeEndret(behov: VedtaksperiodeEndret) {
        if (oppdatertMålingBasertPåVedtaksperiodeId(behov.vedtaksperiodeId) {
            logger.info("Utfyller eksisterende måling ${it.målingId} med Behov=$behov (basert på vedtaksperiodeId)")
            it.leggTil(behov)
        } != null) return

        oppdaterMålingBasertPåEventId(behov.forårsaketAv) {
            logger.info("Utfyller eksisterende måling ${it.målingId} med Behov=$behov (basert på forårsaketAv)")
            it.leggTil(behov)
        }
    }

    private fun håndterSaksbehandlerløsning(behov: Saksbehandlerløsning) {
        oppdaterMålingBasertPåEventId(behov.hendelseId) {
            logger.info("Utfyller eksisterende måling ${it.målingId} med Behov=$behov (basert på hendelseId)")
            it.leggTil(behov)
        }
    }

    private fun håndterOppgaveOpprettet(behov: OppgaveOpprettet) =
        oppdaterMålingBasertPåEventId(behov.hendelseId) {
            logger.info("Utfyller eksisterende måling ${it.målingId} med Behov=$behov (basert på hendelseId)")
            it.leggTil(behov)
        }

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
        override fun toString() = "Måling for godkjenning av flere arbeidsgivere: tok totalt ${Duration.between(startet, ferdig).formater()}\n-> " +
            events.joinToString("\n-> ") {
                if (it.opprettet < startet) "${it.navn} - Før måling startet ($it)"
                else "${it.navn} - ${Duration.between(startet, it.opprettet).formater()} ($it)"
            }
    }

    private class Måling(behov: Godkjenningsbehov) {
        val målingId = UUID.randomUUID()
        private val godkjenningsbehov = mutableListOf(behov)
        private val vedtaksperiodeEndret = mutableListOf<VedtaksperiodeEndret>()
        private val saksbehandlerløsninger = mutableListOf<Saksbehandlerløsning>()
        private val oppgaveOpprettet = mutableListOf<OppgaveOpprettet>()
        private val events get() = godkjenningsbehov + vedtaksperiodeEndret + saksbehandlerløsninger + oppgaveOpprettet

        fun leggTil(vedtaksperiodeEndret: VedtaksperiodeEndret) = this.vedtaksperiodeEndret.add(vedtaksperiodeEndret)
        fun leggTil(godkjenningsbehov: Godkjenningsbehov) = this.godkjenningsbehov.add(godkjenningsbehov)
        fun leggTil(saksbehandlerløsning: Saksbehandlerløsning) = saksbehandlerløsninger.add(saksbehandlerløsning)
        fun leggTil(oppgaveOpprettet: OppgaveOpprettet) = this.oppgaveOpprettet.add(oppgaveOpprettet)

        fun kjennerTilVedtaksperiodeId(vedtaksperiodeId: UUID) =
            godkjenningsbehov.any { it.vedtaksperiodeId == vedtaksperiodeId } ||
            godkjenningsbehov.any { it.aktiveVedtaksperioder.contains(vedtaksperiodeId) } ||
            vedtaksperiodeEndret.any { it.vedtaksperiodeId == vedtaksperiodeId }

        fun kjennerTilEventId(id: UUID) =
            events.any { it.eventIds.contains(id) }

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
        val forårsaketAv = node.forårsaketAv
        val aktiveVedtaksperioder = node["Godkjenning"].path("aktiveVedtaksperioder").map { UUID.fromString(it.path("vedtaksperiodeId").asText()) }.toSet()
        override val eventIds = setOf(id, forårsaketAv)
        init {
            info["vedtaksperiodeId"] = vedtaksperiodeId
            info["forårsaketAv"] = forårsaketAv
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
        override val eventIds = setOf(id, forårsaketAv)
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
        override val eventIds = setOf(id, hendelseId)
        init {
            info["hendelseId"] = hendelseId
        }
    }

    private class OppgaveOpprettet(
        node: JsonNode
    ) : Event(node.id, node.opprettet, node.eventName) {
        val hendelseId = UUID.fromString(node.path("hendelseId").asText())
        override val eventIds = setOf(id, hendelseId)
        init {
            info["hendelseId"] = hendelseId
        }
    }
}