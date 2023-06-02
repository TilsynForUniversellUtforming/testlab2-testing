package no.uutilsynet.testlab2testing

fun Throwable.firstMessage(): String? =
    if (this.cause == null) this.message else this.firstMessage()
