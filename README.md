# Altinn klient

Dette er en klient for å koble seg på fager sitt [altinn-tilganger api](https://github.com/navikt/arbeidsgiver-altinn-tilganger).
Dette bruker vi for å hente ut hvilke organisasjoner en bruker har tilgang til i altinn, og hvilke roller de har i disse organisasjonene. 
Dette brukes igjen for tilgangskontroll i diverse backend applikasjoner feks [fritakagp](https://github.com/navikt/fritakagp) og [simba](https://github.com/navikt/helsearbeidsgiver-inntektsmelding).

## bruk av klienten:

### OBO klient:
```kotlin
        val altinnKlient = Altinn3OBOClient(
            baseUrl = "http://arbeidsgiver-altinn-tilganger.fager",
            ressurs = Altinn3Ressurs.FRITAKAGP,
            cacheConfig = LocalCache.Config(60.minutes, 250)
        )
        val altinnTilganger = altinnKlient.hentAltinnTilganger("12345678910", {"Tokenx token"})
```

### M2M klient:
```kotlin
        val altinnKlient = Altinn3M2MClient(
            baseUrl = "http://arbeidsgiver-altinn-tilganger.fager",
            ressurs = Altinn3Ressurs.FRITAKAGP,
            cacheConfig = LocalCache.Config(60.minutes, 250),
            getToken = {"Entra token"}
        )
        val altinnTilganger = altinnKlient.hentAltinnTilganger("12345678910")
```