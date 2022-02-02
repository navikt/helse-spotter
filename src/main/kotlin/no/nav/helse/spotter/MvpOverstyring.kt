package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal class MvpOverstyring(private val overstyringEvent: String) {
    private val logger = LoggerFactory.getLogger("no.nav.helse.spotter.$overstyringEvent")

    internal fun registrer(rapidsCliApplication: RapidsCliApplication) {
        rapidsCliApplication.apply {
            JsonRiver(this).apply {
                validate(onlyEvents(overstyringEvent))
                validate(textFieldValidation("@id"))
                validate(textFieldValidation("fødselsnummer"))
                validate(erIkkeOvervåket())
                onMessage { _, node -> startOvervåking(node.fnr, node) }
            }
            JsonRiver(this).apply {
                validate(onlyEvents("vedtaksperiode_endret"))
                validate(textFieldValidation("fødselsnummer"))
                validate(textFieldValidation("vedtaksperiodeId"))
                validate(dateTimeValidation("@opprettet"))
                validate(erOvervåket())
                validate(harIkkeKobletVedtaksperiodeTilOverstyring())
                onMessage { _, node -> kobleVedtaksperiodeTilOverstyring(node) }
            }
            JsonRiver(this).apply {
                validate(onlyEvents("vedtaksperiode_endret"))
                validate(textFieldValidation("fødselsnummer"))
                validate(textFieldValidation("vedtaksperiodeId"))
                validate(dateTimeValidation("@opprettet"))
                validate(erOvervåket())
                validate(harKobletVedtaksperiodeTilOverstyring())
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
                    return@validate node.hasNonNull("@final") || !node.path("@final")
                        .asBoolean() // forventer ikke hele svaret
                }
                validate(harKobletVedtaksperiodeTilOverstyring())
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
                validate(harKobletVedtaksperiodeTilOverstyring())
                onMessage { _, node ->
                    kobleOverstyringTilGodkjenningsbehov(
                        node.vedtaksperiodeId,
                        node.id
                    )
                }
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
                        avsluttOvervåking(vedtaksperiodeId, overstyringEvent)
                    }
                }
            }
        }
    }

    private val overvåkingOverstyring = mutableMapOf<UUID, String>() // overstyr_{X}.@id -> fnr
    private val overvåkingOverstyringTilVedtaksperiode =
        mutableMapOf<UUID, UUID>() // vedtaksperiodeId -> overstyr_{X}.@id
    private val overvåkingGodkjenningsbehovTilVedtaksperiode =
        mutableMapOf<UUID, UUID>() // godkjenningsbehov.@id -> vedtaksperiodeId
    private val overvåkingOverstyringTidtaking = mutableMapOf<UUID, MutableList<Triple<UUID, String, LocalDateTime>>>()
    private fun erOvervåket() =
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
            return node.fnr in overvåkingOverstyring.values
        }

    private fun erIkkeOvervåket() =
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
            return node.id !in overvåkingOverstyring
        }

    private fun startOvervåking(fnr: String, node: JsonNode) {
        overvåkingOverstyring[node.id] = fnr
        overvåkingOverstyringTidtaking[node.id] = mutableListOf(Triple(node.id, node.eventName, node.opprettet))
    }

    private fun harIkkeKobletVedtaksperiodeTilOverstyring() =
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
            return node.vedtaksperiodeId !in overvåkingOverstyringTilVedtaksperiode
        }

    private fun harKobletVedtaksperiodeTilOverstyring() =
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
            return node.vedtaksperiodeId in overvåkingOverstyringTilVedtaksperiode
        }

    private fun harKobletOppgaveTilGodkjenningsbehov() =
        fun(record: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
            val godkjenningsbehovId = UUID.fromString(node.path("hendelseId").asText())
            return godkjenningsbehovId in overvåkingGodkjenningsbehovTilVedtaksperiode
        }

    private fun kobleVedtaksperiodeTilOverstyring(node: JsonNode) {
        val overstyringId = UUID.fromString(node.path("@forårsaket_av").path("id").asText())
        if (overstyringId !in overvåkingOverstyring) return
        overvåkingOverstyringTilVedtaksperiode[node.vedtaksperiodeId] = overstyringId
    }

    private fun kobleOverstyringTilGodkjenningsbehov(vedtaksperiodeId: UUID, id: UUID) {
        overvåkingGodkjenningsbehovTilVedtaksperiode[id] = vedtaksperiodeId
    }

    private fun lagreOvervåking(vedtaksperiodeId: UUID, node: JsonNode, eventName: String = node.eventName) {
        val overstyringId = overvåkingOverstyringTilVedtaksperiode[vedtaksperiodeId] ?: return
        overvåkingOverstyringTidtaking[overstyringId]?.add(Triple(node.id, eventName, node.opprettet))
    }

    private fun avsluttOvervåking(vedtaksperiodeId: UUID, overstyringEvent: String) {
        val overstyringId = overvåkingOverstyringTilVedtaksperiode[vedtaksperiodeId] ?: return
        printOvervåking(overstyringId, overstyringEvent)

        overvåkingOverstyring.remove(overstyringId)
        overvåkingOverstyringTidtaking.remove(overstyringId)
        overvåkingOverstyringTilVedtaksperiode.filterValues { it == overstyringId }.keys.forEach {
            overvåkingOverstyringTilVedtaksperiode.remove(it)
        }
        overvåkingGodkjenningsbehovTilVedtaksperiode.filterValues { it == vedtaksperiodeId }.keys.forEach {
            overvåkingGodkjenningsbehovTilVedtaksperiode.remove(it)
        }
    }

    private fun printOvervåking(overstyringId: UUID, overstyringEvent: String) {
        overvåkingOverstyringTidtaking[overstyringId]?.also {
            val første = it.first().third
            val siste = it.last().third
            val tidsbruk = Duration.between(første, siste)
            treghetHistogram.labels(overstyringEvent).observe(tidsbruk.toSeconds().toDouble())
            logger.info("Måling for $overstyringEvent: tok totalt ${tidsbruk.formater()}\n->" + it.joinToString(separator = "\n-> ") { (id, eventName, opprettet) ->
                "$eventName ($id) - ${Duration.between(første, opprettet).formater()}"
            })
        }
    }
}
