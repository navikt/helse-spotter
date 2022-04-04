package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.*
import no.nav.helse.spotter.harStandardfelter
import no.nav.helse.spotter.meldingsoppsamler.målinger.*
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.LocalTime

internal class MeldingsoppsamlerRiver {
    private val konkreteMålinger = listOf(
        OverstyrInntekt,
        OverstyrTidslinje,
        GodkjenningEnArbeidsgiver,
        GodkjenningFlereArbeidsgivere
    )

    private val meldingsoppsamler = Meldingsoppsamler(
        timeoutListener = TimeoutMåling,
        listeners = konkreteMålinger
    )

    internal fun registrer(rapidsCliApplication: RapidsCliApplication) {
        rapidsCliApplication.apply {
            JsonRiver(this).apply {
                validate(harStandardfelter())
                validate(ignorerteEvents())
                validate(spottetid())
                onMessage { _, node -> node.leggTil() }
            }
        }
    }

    private fun JsonNode.leggTil() = kotlin.runCatching {
        meldingsoppsamler.leggTil(Melding(this))
    }.onFailure { throwable ->
        logger.error("Feil ved håndtering av melding ${this.id} (${this.eventName})", throwable)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(Meldingsoppsamler::class.java)
        private val spottetid = LocalTime.parse("05:00:00")..LocalTime.parse("23:59:59")

        private val ignorerteEvents = listOf(
            "ping",
            "pong",
            "subsumsjon",
            "app_status",
            "planlagt_påminnelse",
            "påminnelse",
            "utbetalingpåminnelse",
            "person_påminnelse",
            "infotrygdendring_uten_aktør"
        )

        private fun ignorerteEvents() =
            fun(_: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
                if (node.eventName in ignorerteEvents) return reasons.failed("Ignorer melding med @event_name=${node.eventName}")
                return true
            }

        private fun spottetid() =
            fun(_: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
                if (node.opprettet.toLocalTime() in spottetid) return true
                return reasons.failed("Utenfor spottetid")
            }
    }
}