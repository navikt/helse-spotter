package no.nav.helse.spotter.meldingsoppsamler.målinger

import no.nav.helse.spotter.meldingsoppsamler.Tags.avventerArbeidgivereTilAvventerHistorikk
import no.nav.helse.spotter.meldingsoppsamler.Tags.gjelderEnArbeidsgiver
import no.nav.helse.spotter.meldingsoppsamler.Tags.gjelderFlereArbeidsgivere

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
    erAktuell = { måling ->
        måling.any { it.navn == "behov" && it.tags.gjelderEnArbeidsgiver() }
    }
)

internal object GodkjenningFlereArbeidsgivere : Måling(
    navn = { "godkjenning_av_flere_arbeidsgivere" },
    fra = { it.navn == "saksbehandler_løsning" },
    til = { it.navn == "oppgave_opprettet" },
    erAktuell = { måling ->
        måling.any { it.navn == "behov" && it.tags.gjelderFlereArbeidsgivere() } &&
        måling.any { it.navn == "vedtaksperiode_endret" && it.tags.avventerArbeidgivereTilAvventerHistorikk() }
    }
)