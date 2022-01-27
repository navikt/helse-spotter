package no.nav.helse.spotter

import no.nav.rapids_and_rivers.cli.AivenConfig
import no.nav.rapids_and_rivers.cli.ConsumerProducerFactory
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

fun main() {
    Api().start()

    RapidsCliApplication(ConsumerProducerFactory(AivenConfig.default)).apply {
        MvpOverstyring("overstyr_inntekt").registrer(this)
        MvpOverstyring("overstyr_tidslinje").registrer(this)
    }.start(
        "tbd-spotter-v1",
        listOf("tbd.rapid.v1"),
        Properties().also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest" }
    )
}