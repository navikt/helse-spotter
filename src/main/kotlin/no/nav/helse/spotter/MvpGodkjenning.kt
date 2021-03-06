package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.*

internal class MvpGodkjenning(
    private val målingFerdig: (målingsresultat: Målingsresultat) -> Unit = {
        logger.info(it.toString())
        treghetHistogram.labels(it.navn).observe(it.tidsbruk.toSeconds().toDouble())
    }
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MvpGodkjenning::class.java)
    }

    private val målinger = mutableListOf<Måling>()

    internal fun antallPågåendeMålinger() = målinger.size

    private fun oppdaterMålingBasertPåVedtaksperiodeId(vedtaksperiodeId: UUID, oppdater: (måling: Måling) -> Unit) =
        målinger.firstOrNull { it.kjennerTilVedtaksperiodeId(vedtaksperiodeId) }?.also { måling ->
            oppdater(måling)
        }

    private fun oppdaterMålingBasertPåEventId(eventId: UUID, oppdater: (måling: Måling) -> Unit) =
        målinger.firstOrNull { it.kjennerTilEventId(eventId) }?.also { måling ->
            oppdater(måling)
        }

    private fun ryddIMålinger() {
        målinger.removeIf { måling ->
            måling.kanSlettes().also { slettes ->
                if (slettes) {
                    logger.info("Sletter eksisterende måling ${måling.målingId} som ikke har kommet noen veg på 2 timer" + måling.events.formater())
                }
            }
        }
    }

    private fun håndterGodkjenning(godkjenning: Godkjenning) {
        if (godkjenning.slettMåling) {
            målinger.removeIf {
                it.kjennerTilEventId(godkjenning.id) ||
                it.kjennerTilEventId(godkjenning.forårsaketAv) ||
                it.kjennerTilVedtaksperiodeId(godkjenning.vedtaksperiodeId)
            }
            return ryddIMålinger()
        }

        if (oppdaterMålingBasertPåVedtaksperiodeId(godkjenning.vedtaksperiodeId) {
                logger.info("Utfyller eksisterende måling ${it.målingId} med ${godkjenning.navn}=$godkjenning (basert på vedtaksperiodeId)")
                it.leggTil(godkjenning)
            } != null) return ryddIMålinger()

        if (oppdaterMålingBasertPåEventId(godkjenning.forårsaketAv) {
                logger.info("Utfyller eksisterende måling ${it.målingId} med ${godkjenning.navn}=$godkjenning (basert på forårsaketAv)")
                it.leggTil(godkjenning)
            } != null) return ryddIMålinger()

        if (oppdaterMålingBasertPåEventId(godkjenning.id) {
                logger.info("Utfyller eksisterende måling ${it.målingId} med ${godkjenning.navn}=$godkjenning (basert på eventId)")
                it.leggTil(godkjenning)
            } != null) return ryddIMålinger()

        ryddIMålinger()
    }

    private fun håndterVedtaksperiodeEndret(event: VedtaksperiodeEndret) {
        if (oppdaterMålingBasertPåVedtaksperiodeId(event.vedtaksperiodeId) {
                logger.info("Utfyller eksisterende måling ${it.målingId} med VedtaksperiodeEndret=$event (basert på vedtaksperiodeId)")
                it.leggTil(event)
            } != null) return

        oppdaterMålingBasertPåEventId(event.forårsaketAv) {
            logger.info("Utfyller eksisterende måling ${it.målingId} med VedtaksperiodeEndret=$event (basert på forårsaketAv)")
            it.leggTil(event)
        }
    }

    private fun håndterSaksbehandlerløsning(event: Saksbehandlerløsning) {
        if (målinger.any { it.gjelderSakbehandlingsløsning(event) }) return
        val nyMåling = Måling(event)
        logger.info("Starter ny måling ${nyMåling.målingId} fra Saksbehandlerløsning=$event")
        målinger.add(nyMåling)
    }

    private fun håndterOppgaveOpprettet(event: OppgaveOpprettet) =
        oppdaterMålingBasertPåEventId(event.hendelseId) {
            logger.info("Utfyller eksisterende måling ${it.målingId} med OppgaveOpprettet=$event (basert på hendelseId)")
            it.leggTil(event)
        }

    internal fun registrer(rapidsCliApplication: RapidsCliApplication) {
        rapidsCliApplication.apply {
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
                validate(onlyEvents("behov"))
                validate { _, node, _ -> "Godkjenning" in node.path("@behov").map(JsonNode::asText) }
                validate(textFieldValidation("vedtaksperiodeId"))
                validate { _, node, _ ->
                    node.path("Godkjenning").path("inntektskilde").isTextual
                }
                validate { _, node, _ -> node.path("Godkjenning").path("aktiveVedtaksperioder").isArray }
                onMessage { _, node ->
                    håndterGodkjenning(Godkjenning(node))
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
                validate(onlyEvents("oppgave_opprettet"))
                validate(textFieldValidation("hendelseId"))
                onMessage { _, node ->
                    håndterOppgaveOpprettet(OppgaveOpprettet(node))?.also { måling ->
                        måling.måling().also { målingsresultat ->
                            målingFerdig(målingsresultat)
                            målinger.remove(måling)
                        }
                    }
                }
            }
        }
    }


    data class Målingsresultat(
        val navn: String,
        val events: List<Event>,
        val startet: LocalDateTime,
        val ferdig: LocalDateTime
    ) {
        val tidsbruk = Duration.between(startet, ferdig)
        override fun toString() = "Måling for $navn: tok totalt ${tidsbruk.formater()}" + events.formater()
    }


    private class Måling(
        private val saksbehandlerløsning: Saksbehandlerløsning
    ) {
        val målingId = UUID.randomUUID()
        private val godkjenningsbehov = mutableListOf<Godkjenning>()
        private val vedtaksperiodeEndret = mutableListOf<VedtaksperiodeEndret>()
        private val oppgaveOpprettet = mutableListOf<OppgaveOpprettet>()
        val events get() = godkjenningsbehov + vedtaksperiodeEndret + oppgaveOpprettet + saksbehandlerløsning

        fun leggTil(vedtaksperiodeEndret: VedtaksperiodeEndret) = this.vedtaksperiodeEndret.leggTilEvent(vedtaksperiodeEndret)
        fun leggTil(godkjenning: Godkjenning) = this.godkjenningsbehov.leggTilEvent(godkjenning)
        fun leggTil(oppgaveOpprettet: OppgaveOpprettet) = this.oppgaveOpprettet.leggTilEvent(oppgaveOpprettet)
        fun kanSlettes() = now() > events.maxByOrNull { it.opprettet }!!.opprettet.plusHours(2)

        private fun <T : Event>MutableList<T>.leggTilEvent(event: T) {
            if (any { it.id == event.id }) return
            add(event)
        }

        fun kjennerTilVedtaksperiodeId(vedtaksperiodeId: UUID) =
            godkjenningsbehov.any { it.vedtaksperiodeId == vedtaksperiodeId } ||
                godkjenningsbehov.any { it.aktiveVedtaksperioder.contains(vedtaksperiodeId) } ||
                vedtaksperiodeEndret.any { it.vedtaksperiodeId == vedtaksperiodeId }

        fun kjennerTilEventId(id: UUID) =
            events.any { it.eventIds.contains(id) }

        fun gjelderSakbehandlingsløsning(saksbehandlerløsning: Saksbehandlerløsning) =
            this.saksbehandlerløsning.id == saksbehandlerløsning.id

        fun måling(): Målingsresultat {
            val sortertPåTid = events.sortedBy { it.opprettet }
            val navn = when (godkjenningsbehov.any { it.flereArbeidsgivere }) {
                true -> "GodkjenningFlereArbeidsgivere"
                false -> "GodkjenningEnArbeidsgiver"
            }

            return Målingsresultat(
                navn = navn,
                startet = saksbehandlerløsning.opprettet,
                ferdig = sortertPåTid.last().opprettet,
                events = sortertPåTid
            )
        }
    }

    private class Godkjenning(
        node: JsonNode
    ) : Event(node.id, node.godkjenttidspunktEllerOpprettet(), node.eventNavn()) {
        val vedtaksperiodeId = node.vedtaksperiodeId
        val forårsaketAv = node.forårsaketAv
        val aktiveVedtaksperioder = node.path("Godkjenning").path("aktiveVedtaksperioder")
            .map { UUID.fromString(it.path("vedtaksperiodeId").asText()) }.toSet()
        val flereArbeidsgivere = node.path("Godkjenning").path("inntektskilde").asText() == "FLERE_ARBEIDSGIVERE"
        val slettMåling = node.harLøsning() && aktiveVedtaksperioder.isEmpty()
        override val eventIds = setOf(id, forårsaketAv)

        init {
            info["vedtaksperiodeId"] = vedtaksperiodeId
            info["forårsaketAv"] = forårsaketAv
            info["aktiveVedtaksperioder"] = aktiveVedtaksperioder
        }

        private companion object {
            private const val BEHOV = "Godkjenningsbehov"
            private const val LØSNING = "Godkjenningsløsning"
            private fun JsonNode.harLøsning() = path("@løsning").path("Godkjenning").isObject
            private fun JsonNode.godkjenttidspunktEllerOpprettet() = when (harLøsning()) {
                true -> LocalDateTime.parse(path("@løsning").path("Godkjenning").path("godkjenttidspunkt").asText())
                false -> opprettet
            }
            private fun JsonNode.eventNavn() = when (harLøsning()) {
                true -> LØSNING
                false -> BEHOV
            }
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

private fun List<Event>.formater(): String {
    if (isEmpty()) return "tom"
    val første = first()
    return "\n-> " + joinToString("\n-> ") {
        if (it == første) "${it.navn} - Start ($it)"
        else "${it.navn} - ${Duration.between(første.opprettet, it.opprettet).formater()} ($it)"
    }
}
