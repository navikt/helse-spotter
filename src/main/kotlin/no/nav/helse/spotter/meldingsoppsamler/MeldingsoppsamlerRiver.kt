package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spotter.*
import no.nav.helse.spotter.harStandardfelter
import no.nav.helse.spotter.id
import no.nav.helse.spotter.meldingsoppsamler.Deltaker.Companion.resolveDeltaker
import no.nav.helse.spotter.meldingsoppsamler.målinger.*
import no.nav.rapids_and_rivers.cli.JsonRiver
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerRecord

internal class MeldingsoppsamlerRiver {
    private val meldingsoppsamler = Meldingsoppsamler(
        LøseBehovForHistorikk,
        OverstyrInntekt,
        OverstyrTidslinje,
        GodkjenningEnArbeidsgiver,
        GodkjenningFlereArbeidsgivere
    )

    internal fun registrer(rapidsCliApplication: RapidsCliApplication) {
        rapidsCliApplication.apply {
            JsonRiver(this).apply {
                validate(harStandardfelter())
                validate(rejectEvents(
                    "ping",
                    "pong",
                    "subsumsjon",
                    "app_status"
                ))
                onMessage { _, node ->
                    meldingsoppsamler.leggTil(Melding(
                        id = node.id,
                        deltaker = node.resolveDeltaker(),
                        navn = node.eventName,
                        payload = node.toString()
                    ))
                }
            }
        }
    }

    private companion object {

        private fun rejectEvents(vararg events: String) =
            events.toList().let { rejectedEvents ->
                fun (_: ConsumerRecord<String, String>, node: JsonNode, reasons: MutableList<String>): Boolean {
                    if (node.eventName in rejectedEvents) return reasons.failed("Ignorer melding med @event_name=${node.eventName}")
                    return true
                }
            }
    }
}