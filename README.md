# Monorepo for mikrotjenester i team Toi sin rapids-and-rivers arkitektur

## Republisering
For republisering på Kafka av all kandidatinformasjon, se README i toi-sammenstille-kandidat. 

## For å kunne bygge 
Bygget trenger å laste ned noen pakker fra Github Pacakge Registry. Slik setter du opp credentials for å autentisere deg:

1. Du trenger å generere en Personal Access Token i Github som har read-packages-scope. 
2. Bygg-scriptet leter etter gradle-propertien `passwordGithub`. Den setter du i filen `gradle.properties` på hjemmeområdet ditt. Default katalog er `~/.gradle/`. Den skal ikke settes i "gradle.properties" som ligger i prosjektet, fordi dette er din personlige secret. 
3. Frivillig: Jeg har lagret min Personal Access Token i en miljøvariabel `GITHUB_TOKEN` i operativsystemet mitt. Jeg trenger den i flere andre sammenhenger og jeg vil helst å lagre den bare ett sted på min maskin for å redusere sannsynligheten litt for at den kommer på avveie. Jeg kan refere til miljøvariabelen i min fil `~/.gradle/gradle.properties`, som inneholder kun denne ene linjen:
```
passwordGithub=GITHUB_TOKEN
```


# Henvendelser

## For Nav-ansatte

* Dette Git-repositoriet eies
  av [team Toi i produktområde Arbeidsgiver](https://teamkatalog.nav.no/team/76f378c5-eb35-42db-9f4d-0e8197be0131).
* Slack-kanaler:
    * [#arbeidsgiver-toi-dev](https://nav-it.slack.com/archives/C02HTU8DBSR)
    * [#rekrutteringsbistand-værsågod](https://nav-it.slack.com/archives/C02HWV01P54)

## For folk utenfor Nav

IT-avdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)
