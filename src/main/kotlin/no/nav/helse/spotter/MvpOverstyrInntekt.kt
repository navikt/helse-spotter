package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.prometheus.client.Histogram
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

private val logger = LoggerFactory.getLogger("no.nav.helse.spotter.OverstyrInntekt")
private val treghetHistogram = Histogram.build("treghet", "Måling av treghet")
    .buckets(1.00, 2.00, 5.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 50.00, 60.00, 120.00, 300.00)
    .labelNames("type")
    .register()

internal fun RapidsCliApplication.mvpOverstyrInntekt() {
    JsonRiver(this).apply {
        validate(onlyEvents("overstyr_inntekt"))
        validate(textFieldValidation("@id"))
        validate(textFieldValidation("fødselsnummer"))
        validate(erIkkeOvervåket())
        onMessage { _, node -> startOvervåking(node.fnr, node) }
        //register(consumeAndObserveBehov("Simulering", "Utbetaling"))
        // register(consumeAndObserveId(behovId))
    }
    JsonRiver(this).apply {
        validate(onlyEvents("vedtaksperiode_endret"))
        validate(textFieldValidation("fødselsnummer"))
        validate(textFieldValidation("vedtaksperiodeId"))
        validate(dateTimeValidation("@opprettet"))
        validate(erOvervåket())
        validate(harIkkeKobletVedtaksperiodeTilOverstyrInntekt())
        onMessage { _, node -> kobleVedtaksperiodeTilOverstyrInntekt(node) }
    }
    JsonRiver(this).apply {
        validate(onlyEvents("vedtaksperiode_endret"))
        validate(textFieldValidation("fødselsnummer"))
        validate(textFieldValidation("vedtaksperiodeId"))
        validate(dateTimeValidation("@opprettet"))
        validate(erOvervåket())
        validate(harKobletVedtaksperiodeTilOverstyrInntekt())
        onMessage { _, node -> lagreOvervåking(node.vedtaksperiodeId, node) }
    }
    JsonRiver(this).apply {
        validate(onlyEvents("behov"))
        validate(textFieldValidation("fødselsnummer"))
        validate(textFieldValidation("vedtaksperiodeId"))
        validate(dateTimeValidation("@opprettet"))
        validate { _, node, _ ->
            if (!node.hasNonNull("@løsning")) return@validate false // forventer løsning
            if (!node.path("@løsning").isObject) return@validate false // forventer at @løsning er JsonObject
            if ((node.path("@løsning") as ObjectNode).size() != 1) return@validate false // forventer kun én løsning
            return@validate node.hasNonNull("@final") || !node.path("@final").asBoolean() // forventer ikke hele svaret
        }
        validate(harKobletVedtaksperiodeTilOverstyrInntekt())
        onMessage { _, node ->
            lagreOvervåking(
                node.vedtaksperiodeId,
                node,
                (node.path("@løsning") as ObjectNode).fieldNames().next()
            )
        }
    }
    JsonRiver(this).apply {
        validate(onlyEvents("behov"))
        validate { _, node, _ -> "Godkjenning" in node.path("@behov").map(JsonNode::asText) }
        validate { _, node, _ -> !node.hasNonNull("@løsning") } // bare uløste behov
        validate(textFieldValidation("@id"))
        validate(textFieldValidation("vedtaksperiodeId"))
        validate(dateTimeValidation("@opprettet"))
        validate(harKobletVedtaksperiodeTilOverstyrInntekt())
        onMessage { _, node -> kobleOverstyrInntektTilGodkjenningsbehov(node.vedtaksperiodeId, node.id) }
        onMessage { _, node ->
            lagreOvervåking(
                node.vedtaksperiodeId,
                node,
                (node.path("@løsning") as ObjectNode).fieldNames().next()
            )
        }
    }
    JsonRiver(this).apply {
        validate(onlyEvents("oppgave_opprettet"))
        validate(textFieldValidation("@id"))
        validate(textFieldValidation("hendelseId"))
        validate(dateTimeValidation("@opprettet"))
        validate(harKobletOppgaveTilGodkjenningsbehov())
        onMessage { _, node ->
            overvåkingGodkjenningsbehovTilVedtaksperiode[UUID.fromString(
                node.path("hendelseId").asText()
            )]?.also { vedtaksperiodeId ->
                lagreOvervåking(vedtaksperiodeId, node)
                avsluttOvervåking(vedtaksperiodeId)
            }
        }
    }
}

private val overvåkingOverstyrInntekter = mutableMapOf<UUID, String>() // overstyr_inntekt.@id -> fnr
private val overvåkingOverstyrInntekterTilVedtaksperiode =
    mutableMapOf<UUID, UUID>() // vedtaksperiodeId -> overstyr_inntekt.@id
private val overvåkingGodkjenningsbehovTilVedtaksperiode =
    mutableMapOf<UUID, UUID>() // godkjenningsbehov.@id -> vedtaksperiodeId
private val overvåkingOverstyrInntektTidtaking = mutableMapOf<UUID, MutableList<Triple<UUID, String, LocalDateTime>>>()
private fun erOvervåket() =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        return node.fnr in overvåkingOverstyrInntekter.values
    }

private fun erIkkeOvervåket() =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        return node.id !in overvåkingOverstyrInntekter
    }

private val JsonNode.id get() = UUID.fromString(path("@id").asText())
private val JsonNode.fnr get() = path("fødselsnummer").asText()
private val JsonNode.opprettet get() = LocalDateTime.parse(path("@opprettet").asText())
private val JsonNode.eventName get() = path("@event_name").asText()
private val JsonNode.vedtaksperiodeId get() = UUID.fromString(path("vedtaksperiodeId").asText())

private fun startOvervåking(fnr: String, node: JsonNode) {
    overvåkingOverstyrInntekter[node.id] = fnr
    overvåkingOverstyrInntektTidtaking[node.id] = mutableListOf(Triple(node.id, node.eventName, node.opprettet))
}

private fun harIkkeKobletVedtaksperiodeTilOverstyrInntekt() =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        return node.vedtaksperiodeId !in overvåkingOverstyrInntekterTilVedtaksperiode
    }

private fun harKobletVedtaksperiodeTilOverstyrInntekt() =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        return node.vedtaksperiodeId in overvåkingOverstyrInntekterTilVedtaksperiode
    }

private fun harKobletOppgaveTilGodkjenningsbehov() =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        val godkjenningsbehovId = UUID.fromString(node.path("hendelseId").asText())
        return godkjenningsbehovId in overvåkingGodkjenningsbehovTilVedtaksperiode
    }

private fun kobleVedtaksperiodeTilOverstyrInntekt(node: JsonNode) {
    val overstyrInntektId = UUID.fromString(node.path("@forårsaket_av").path("id").asText())
    if (overstyrInntektId !in overvåkingOverstyrInntekter) return
    overvåkingOverstyrInntekterTilVedtaksperiode[node.vedtaksperiodeId] = overstyrInntektId
}

private fun kobleOverstyrInntektTilGodkjenningsbehov(vedtaksperiodeId: UUID, id: UUID) {
    overvåkingGodkjenningsbehovTilVedtaksperiode[id] = vedtaksperiodeId
}

private fun lagreOvervåking(vedtaksperiodeId: UUID, node: JsonNode, eventName: String = node.eventName) {
    val overstyrInntektId = overvåkingOverstyrInntekterTilVedtaksperiode[vedtaksperiodeId] ?: return
    overvåkingOverstyrInntektTidtaking[overstyrInntektId]?.add(Triple(node.id, eventName, node.opprettet))
}

private fun avsluttOvervåking(vedtaksperiodeId: UUID) {
    val overstyrInntektId = overvåkingOverstyrInntekterTilVedtaksperiode[vedtaksperiodeId] ?: return
    printOvervåking(overstyrInntektId)

    overvåkingOverstyrInntekter.remove(overstyrInntektId)
    overvåkingOverstyrInntektTidtaking.remove(overstyrInntektId)
    overvåkingOverstyrInntekterTilVedtaksperiode.remove(vedtaksperiodeId)
    overvåkingGodkjenningsbehovTilVedtaksperiode.filterValues { it == vedtaksperiodeId }.keys.forEach {
        overvåkingGodkjenningsbehovTilVedtaksperiode.remove(it)
    }
}

private fun printOvervåking(overstyrInntektId: UUID) {
    overvåkingOverstyrInntektTidtaking[overstyrInntektId]?.also {
        val første = it.first().third
        val siste = it.last().third
        val tidsbruk = Duration.between(første, siste)
        treghetHistogram.labels("overstyr_inntekt").observe(tidsbruk.toSeconds().toDouble())
        logger.info("Tidsbruk for overstyr_inntekt: $tidsbruk")
        logger.info(it.joinToString(separator = " -> ") { (id, eventName, opprettet) ->
            "$eventName ($id) - ${Duration.between(første, opprettet)}"
        })
    }
}

private fun MutableList<String>.failed(why: String) = false.also { this.add(why) }
private fun Boolean.ifFailed(reasons: MutableList<String>, why: String) = if (this) true else reasons.failed(why)

private val eventValidation =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        if (!node.hasNonNull("@event_name")) return reasons.failed("Inneholder ikke @event_name")
        if (!node.path("@event_name").isTextual) return reasons.failed("@event_name er ikke tekstlig")
        return true
    }

private fun textFieldValidation(field: String) =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        if (!node.hasNonNull(field)) return reasons.failed("Inneholder ikke $field")
        if (!node.path(field).isTextual) return reasons.failed("$field er ikke tekstlig")
        return true
    }

private fun dateTimeValidation(field: String) =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        if (!textFieldValidation(field)(record, node, reasons)) return false
        return try {
            LocalDateTime.parse(node.path(field).asText())
            true
        } catch (err: Exception) {
            reasons.add("$field er ikke localdatetime")
            false
        }
    }

private fun onlyEvents(vararg type: String) =
    type.toList().let { typer ->
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>) =
            eventValidation(record, node, reasons) && (node.path("@event_name").asText() in typer).ifFailed(
                reasons,
                "@event_name er ikke av forventet type"
            )
    }