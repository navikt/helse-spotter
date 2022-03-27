package no.nav.helse.spotter.meldingsoppsamler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UuidLookupTest {

    @Test
    fun `finner aller uuider i en melding`() {
        @Language("JSON")
        val melding = """
        {
            "root": "1ea0c9c4-6861-44e3-aaaf-7787d5fffdc9",
            "object": {
                "uuid": "5507a241-6f93-4534-81b3-3a7e30e6fad2",
                "object": {
                    "uuid": "07f7edee-1a54-4c2e-a483-ddb624149151"
                }
            },
            "list": ["e61e1c35-cbed-43c4-aa79-08d8c205bcf6", "af0cbef3-0719-4a7e-8ffd-dee5d36c4b7e"],
            "objectList": [{
                "uuid": "e31bcc38-fd0c-4ddb-ad92-1fd98dbe78e7"
            }, {
                "uuid": "1ea0c9c4-6861-44e3-aaaf-7787d5fffdc9"
            }]
        }
        """

        val forventet = listOf(
            "1ea0c9c4-6861-44e3-aaaf-7787d5fffdc9",
            "5507a241-6f93-4534-81b3-3a7e30e6fad2",
            "07f7edee-1a54-4c2e-a483-ddb624149151",
            "e61e1c35-cbed-43c4-aa79-08d8c205bcf6",
            "af0cbef3-0719-4a7e-8ffd-dee5d36c4b7e",
            "e31bcc38-fd0c-4ddb-ad92-1fd98dbe78e7"
        ).map { UUID.fromString(it) }.toSet()

        assertEquals(forventet, UuidLookup.lookup(melding))
        assertEquals(forventet, UuidLookup.lookup(jacksonObjectMapper().writeValueAsString(melding)))
    }

    @Test
    fun `melding uten uuider`() {
        assertEquals(emptySet<UUID>(), UuidLookup.lookup(""))
        assertEquals(emptySet<UUID>(), UuidLookup.lookup("1ea0c9c4_6861_44e3_aaaf_7787d5fffdc9"))
    }
}