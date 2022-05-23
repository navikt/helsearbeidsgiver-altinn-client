package no.nav.helsearbeidsgiver.altinn

fun String.loadFromResources(): String {
    return ClassLoader.getSystemResource(this).readText()
}
