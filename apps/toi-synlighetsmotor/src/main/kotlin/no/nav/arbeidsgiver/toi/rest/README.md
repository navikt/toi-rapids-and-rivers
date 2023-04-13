# API dokumentasjon

## /evaluering
### Autentisering
#### Veiledertoken (Azure obo token)
#### Token audience applikasjon: toi-synlighetsmotor, cluster: dev-gcp/prod-gcp team: toi
### Url dev
#### GET https://toi-synlighetsmotor.intern.dev.nav.no/evaluering
### Url prod
#### GET https://toi-synlighetsmotor.intern.nav.no/evaluering

## /synlighet
### Autentisering
#### Arbeidsgivertoken (TokenX obo token)
#### Token audience applikasjon: toi-synlighetsmotor, cluster: dev-gcp/prod-gcp team: toi 
### Url dev
#### POST https://toi-synlighetsmotor.intern.dev.nav.no/synlighet
### Url prod
#### POST https://toi-synlighetsmotor.intern.nav.no/synlighet
### Inputbody
#### Format: ```List<String>``` Der 'String' er fødselsnummer
#### Eksempel Json: ```["12345678912","10000000000"]```
### Response
#### Format: ```Map<String, Boolean>``` Der 'String' er fødselsnummer
#### Eksempel Json: ```{"12345678912":true,"10000000000":false}```
### Regler
#### Vi returnerer synlighet true dersom alle kriteriene for synlighet er oppfyllt.
#### Vi returnerer synlighet false dersom ikke alle kriterier er oppfylt, og også om fødselsnummer ikke finnes i systemet vårt.
