package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Histogram
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal val treghetHistogram = Histogram.build("treghet", "Måling av treghet")
    .buckets(1.00, 2.00, 5.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 50.00, 60.00, 120.00, 300.00)
    .labelNames("type")
    .register()

internal fun MutableList<String>.failed(why: String) = false.also { this.add(why) }
internal fun Boolean.ifFailed(reasons: MutableList<String>, why: String) = if (this) true else reasons.failed(why)

internal val eventValidation =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        if (!node.hasNonNull("@event_name")) return reasons.failed("Inneholder ikke @event_name")
        if (!node.path("@event_name").isTextual) return reasons.failed("@event_name er ikke tekstlig")
        return true
    }

internal fun textFieldValidation(field: String) =
    fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
        if (!node.hasNonNull(field)) return reasons.failed("Inneholder ikke $field")
        if (!node.path(field).isTextual) return reasons.failed("$field er ikke tekstlig")
        return true
    }

internal fun dateTimeValidation(field: String) =
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

internal fun onlyEvents(vararg type: String) =
    type.toList().let { typer ->
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>) =
            eventValidation(record, node, reasons) && (node.path("@event_name").asText() in typer).ifFailed(
                reasons,
                "@event_name er ikke av forventet type"
            )
    }

internal fun Duration.formater() = "${toSeconds()} sekunder & ${toMillisPart()} millisekunder"

internal val JsonNode.id get() = UUID.fromString(path("@id").asText())
internal val JsonNode.fnr get() = path("fødselsnummer").asText()
internal val JsonNode.opprettet get() = LocalDateTime.parse(path("@opprettet").asText())
internal val JsonNode.eventName get() = path("@event_name").asText()
internal val JsonNode.vedtaksperiodeId get() = UUID.fromString(path("vedtaksperiodeId").asText())