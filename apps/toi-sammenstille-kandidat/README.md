# Sammenstiller kandidatinformasjon

Henter ut CV og veilederinformasjon, lagrer i database, og legger sammenstillt kandidatinformasjon tilbake på rapid

## Republisering

### Passord 
- Passord for republisering hentes fra NAIS: `kubectl get secret passord-for-republisering -n toi -o jsonpath="{ .data['PASSORD_FOR_REPUBLISERING']}" | base64 -d`
- kan brukes slik på kommandolinja: `jo passord=<passord> | curl -v --json @- <URL>`

### Republisering av alle kandidater
Republisering for alle kandidater leser alle radene i databasetabellen og legger de som meldinger på rapid.

Gjør en POST-request til 
 - dev: `https://toi-sammenstille-kandidat.intern.dev.nav.no/republiser`
 - prod: `https://toi-sammenstille-kandidat.intern.nav.no/republiser`

Med følgende body:

    {
	  "passord": "<passord>"
    }

### Republisering av én kandidat
Republisering av én kandidat henter den aktuelle kandidaten fra databasen og republiserer på rapid. Endepunktet gir 404 ved ugyldig aktørId.

Gjør en POST-request til
- dev: `https://toi-sammenstille-kandidat.intern.dev.nav.no/republiser/<aktørid>`
- prod: `https://toi-sammenstille-kandidat.intern.nav.no/republiser/<aktørid>`

Med følgende body:

    {
	  "passord": "<passord>"
    }
