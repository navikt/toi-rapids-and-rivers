# Leser meldinger om notifikasjon og sender notifikasjon til Min Side Arbeidsgiver

Appen leser notifikasjonsmeldinger fra Rapid og gjør kall til [arbeidsgiver-notifikasjon-produsent-api](https://github.com/navikt/arbeidsgiver-notifikasjon-produsent-api) som sørger for at arbeidsgiver mottar en e-post med generisk beskjed om nye beskjeder, og med et varsel inne på Min Side - Arbeidsgiver med en lenke til den aktuelle kandidatlista.

Appen støtter kun notifikasjon for CV delt med arbeidsgiver.

For at en melding skal plukkes opp av appen må meldinga ha følgende format:

```
    {
      "@event_name": "notifikasjon.cv-delt",
      "notifikasjonsId": "enEllerAnnenId",
      "arbeidsgiversEpostadresser": ["test@testepost.no", "test2@testpost.no"], 
      "stillingsId": "666028e2-d031-4d53-8a44-156efc1a3385",
      "virksomhetsnummer": "123456789",
      "utførtAvVeilederFornavn": "Veileder",
      "utførtAvVeilederEtternavn": "Veiledersen",
      "tidspunktForHendelse": "2022-11-09T10:37:45.108+01:00",
      "meldingTilArbeidsgiver": "Her har du noen fine kandidater!",
      "stillingstittel": "En fantastisk stilling!"
    }
```

arbeidsgiver-notifikasjon-produsent-api er idempotent så lenge innholdet i meldingen er lik.
Dersom notifikasjonsId er lik i to meldinger mens resten av innholdet er ulikt, så vil arbeidsgiver-notifikasjon-produsent-api kaste feil.
Det vil si at man kan sende flere meldinger på rapid'en med samme notifikasjonsId uten at det vil medføre at arbeidsgiver spammes ned av notifikasjoner.

# Notifikasjonsapi grafana, applikasjonens nottifikasjoner har navnet "kandidater"
https://grafana.nais.io/d/_CEEMTr7z/bruk-av-ekstern-varsling?orgId=1&var-cluster=prod-gcp&var-produsent_id=All&var-status=All&var-feilkode=All&var-merkelapp=All

