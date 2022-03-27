package no.nav.helse.spotter.meldingsoppsamler.målinger

internal object OverstyrTidslinje : Måling("overstyr_tidslinje", "oppgave_opprettet")

internal object OverstyrInntekt : Måling("overstyr_inntekt", "oppgave_opprettet")

internal object GodkjenningEnArbeidsgiver : Måling("saksbehandler_løsning", "oppgave_opprettet", {
    måling -> måling.first().payload.contains("EN_ARBEIDSGIVER")
})

internal object GodkjenningFlereArbeidsgivere : Måling("saksbehandler_løsning", "oppgave_opprettet", {
    måling -> måling.first().payload.contains("FLERE_ARBEIDSGIVERE")
})