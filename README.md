# Monorepo for mikrotjenester i team Toi sin rapids-and-rivers arkitektur

Dette repoet inneholder mange apper, som har sin kildekode i hver sin katalog under katelogen `apps`. Hver app har sin egen README-fil.

## Republisering
For republisering på Kafka av all kandidatinformasjon, se README i toi-sammenstille-kandidat. 

## Bygging

### Lokalt
Vi har ingen måte å bygge bare én app på. Bygging gjøres ved å stå i hovedkatalogen og kjøre `./gradlew clean build`.

### På Github
Bygging på Github styres av en wrokflow-fil for hver app i katalogen `.github/workflows`. De gjenbruker filen `deploy-toi-template.yaml` **med unntak av deploy-toi-helseapp.yaml** som har sin egen byggkonfig (per november 2024).

## Trivy security scan
Resulatene/issuene fra scans av alle appene i dette Github-repoet vises sammen i [samme liste](https://github.com/navikt/toi-rapids-and-rivers/security/code-scanning), uten at vi kan se hvilken av appene de kommer fra. Det er ikke optimalt, men det koster mer enn det smaker nå (november 2024) å gjøre noe med det.

Per november 2024 har vi ikke noen kjøring av [Trivy security scan](https://sikkerhet.nav.no/docs/verktoy/trivy) som starter regelmessig og uavhengig av om appen har blitt endret (scheduled trigger). Det betyr at hvis det oppdages en ny sikkerhetsissue der ute i verden som legges inn i Trivy sin database så får vi ikke sjekket appen vår for denne issuen uten å gjøre en endring i appen. For å utløse en Trivy scan i alle appene - med unntak av toi-helseapp - gjør en triviell, ikke-funksjonell endring i en fil de har felles gjennom Github workflow konfigurasjonen sin, som er:
```
- .github/workflows/deploy-toi-template.yaml
- buildSrc/**
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
