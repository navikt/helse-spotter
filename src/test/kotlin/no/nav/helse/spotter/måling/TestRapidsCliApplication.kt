package no.nav.helse.spotter.måling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.rapids_and_rivers.cli.MessageListener
import no.nav.rapids_and_rivers.cli.RapidsCliApplication
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.util.*

internal class TestRapidsCliApplication {
    private val messageListenerSlot = slot<MessageListener>()
    private val messageListeners = mutableSetOf<MessageListener>()

    internal val rapidsCliApplication = mockk<RapidsCliApplication>().also { rapidsCliApplication ->
        every { rapidsCliApplication.register(capture(messageListenerSlot)) }.answers {
            messageListeners.add(messageListenerSlot.captured)
            rapidsCliApplication
        }
    }

    internal fun send(event: String) {
        messageListeners.forEach { messageListener ->
            messageListener.onMessage(ConsumerRecord("topic", 1, 1L, "${UUID.randomUUID()}", event))
        }
    }
}