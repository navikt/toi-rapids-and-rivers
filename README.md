# Tiltak og inkludering's rapids & rivers-microservicer

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
* Dette Git-repositoriet eies av [Team tiltak og inkludering (TOI) i Produktområde arbeidsgiver](https://teamkatalog.nais.adeo.no/team/0150fd7c-df30-43ee-944e-b152d74c64d6).
* Slack-kanaler:
    * [#arbeidsgiver-toi-dev](https://nav-it.slack.com/archives/C02HTU8DBSR)
    * [#arbeidsgiver-utvikling](https://nav-it.slack.com/archives/CD4MES6BB)

## For folk utenfor Nav
* Opprett gjerne en issue i Github for alle typer spørsmål
* IT-utviklerne i Github-teamet https://github.com/orgs/navikt/teams/toi
* IT-avdelingen i [Arbeids- og velferdsdirektoratet](https://www.nav.no/no/NAV+og+samfunn/Kontakt+NAV/Relatert+informasjon/arbeids-og-velferdsdirektoratet-kontorinformasjon)
