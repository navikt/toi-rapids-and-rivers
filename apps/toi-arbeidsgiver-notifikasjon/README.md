# Leser meldinger om notifikasjon og sender notifikasjon til Min Side Arbeidsgiver

Appen leser notifikasjonsmeldinger fra Rapid og gjør kall til [arbeidsgiver-notifikasjon-produsent-api](https://github.com/navikt/arbeidsgiver-notifikasjon-produsent-api) som sørger for at arbeidsgiver mottar en e-post med generisk beskjed om nye beskjeder, og med et varsel inne på Min Side - Arbeidsgiver med en lenke til den aktuelle kandidatlista.

Appen støtter kun notifikasjon for CV delt med arbeidsgiver.

For at en melding skal plukkes opp av appen må meldinga ha følgende format:

```
{
  "@event_name": "notifikasjon.cv-delt",
  "notifikasjonsId": "enUnikId",
  "stillingsId": "656028f2-d031-4d53-8a44-156efc1a2385",
  "virksomhetsnummer": "123456789",
  "utførendeVeilederFornavn": "Veileder",
  "utførendeVeilederEtternavn": "Veiledersen",
  "mottakerEpost": "test@testepost.no",
}
```

arbeidsgiver-notifikasjon-produsent-api er idempotent så lenge innholdet i meldingen er lik.
Dersom notifikasjonsId er lik i to meldinger mens resten av innholdet er ulikt, så vil arbeidsgiver-notifikasjon-produsent-api kaste feil.
Det vil si at man kan sende flere meldinger på rapid'en med samme notifikasjonsId uten at det vil medføre at arbeidsgiver spammes ned av notifikasjoner.

