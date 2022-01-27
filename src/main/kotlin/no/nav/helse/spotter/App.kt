package no.nav.helse.spotter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

fun main() {
    Api().start()
    //val målinger = Målinger()

    RapidsCliApplication(ConsumerProducerFactory(AivenConfig.default)).apply {
        //mvpOverstyrInntekt()
        mvpOverstyring("overstyr_inntekt")
        mvpOverstyring("overstyr_tidslinje")
        /*JsonRiver(this).apply {
            validate { _, jsonNode, _ -> jsonNode["@event_name"].erSatt() }
            validate { _, jsonNode, _ -> jsonNode["@id"].erSatt() }
            validate { _, jsonNode, _ -> jsonNode["@opprettet"].erSatt() }
            //onMessage { _, jsonNode -> målinger.håndter(jsonNode)}
        }*/
    }.start(
        "tbd-spotter-v1",
        listOf("tbd.rapid.v1"),
        Properties().also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest" }
    )

}

private fun JsonNode.isMissingOrNull() = isMissingNode || isNull
private fun JsonNode.erSatt() = !isMissingOrNull()

