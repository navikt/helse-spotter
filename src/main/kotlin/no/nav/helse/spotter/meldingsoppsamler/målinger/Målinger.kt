package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.TagResolver.typedTag

internal object OverstyrTidslinje : Måling(
    navn = { "overstyr_tidslinje" },
    fra = { it.navn == "overstyr_tidslinje" },
    til = { it.navn == "oppgave_opprettet" }
)

internal object OverstyrInntekt : Måling(
    navn = { "overstyr_inntekt" },
    fra = { it.navn == "overstyr_inntekt" },
    til = { it.navn == "oppgave_opprettet" }
)

internal object GodkjenningEnArbeidsgiver : Måling(
    navn = { "godkjenning_av_en_arbeidsgiver" },
    fra = { it.navn == "saksbehandler_løsning" },
    til = { it.navn == "oppgave_opprettet" },
    erAktuell = { måling -> måling.any { it.tags.typedTag("enArbeidsgiver") }
})

internal object GodkjenningFlereArbeidsgivere : Måling(
    navn = { "godkjenning_av_flere_arbeidsgivere" },
    fra = { it.navn == "saksbehandler_løsning" },
    til = { it.navn == "oppgave_opprettet" },
    erAktuell = { måling -> måling.any { it.tags.typedTag("flereArbeidsgivere") }
})