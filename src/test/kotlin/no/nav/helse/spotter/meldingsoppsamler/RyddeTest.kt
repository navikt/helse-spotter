package no.nav.helse.spotter.meldingsoppsamler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RyddeTest {
    @Test
    fun `rydder kun hvert 5 min`() {
        val ryddet = mutableListOf<Int>()
        (0..59).forEach { minutt ->
            if (minutt % 5 == 0) ryddet.add(minutt)
        }
        assertEquals(listOf(0,5,10,15,20,25,30,35,40,45,50,55), ryddet)
    }
}