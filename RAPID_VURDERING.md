# Vurdering: Én delt rapid vs. flere dedikerte topics

## Hva rapiden (én topic) faktisk brukes til positivt

### 1. Det voksende dokumentet (`@behov`-kjeden)

Dette er den viktigste fordelen. Synlighetsmotoren legger ut en melding med:
```json
{ "aktørId": "123", "@behov": ["veileder", "oppfølgingsinformasjon", "kvp", ...] }
```

Hver beriker plukker opp meldingen basert på **første uløste behov**, fyller inn sitt felt, og republiserer. Dokumentet vokser for hvert hopp. Denne mekanismen krever at alle berikere deler topic — ellers måtte du ha eksplisitt ruting mellom topics for hvert steg.

### 2. Multi-konsument-mønster

Samme melding fra f.eks. `toi-kvp` plukkes opp av **både** `toi-sammenstille-kandidat` (lagrer) og `toi-synlighetsmotor` (evaluerer). Med separate topics måtte du publisere til flere topics eller ha en fan-out-mekanisme.

### 3. Ingen konfigurasjon ved ny lytter

Legger du til en ny app som skal reagere på en hendelse? Bare deploy — den plukker opp det den vil fra rapiden. Ingen topic-opprettelse, ingen ACL-endring.

---

## Men: Gir dette reell verdi *internt* i dette repoet?

**Nei, ikke egentlig.** Her er poenget:

- Alle de 24 appene eies av **samme team**
- De deployes fra **samme repo**
- De har **ingen uavhengige livssykluser**
- Behovskjeden er **alltid den samme sekvensen**

Det voksende dokumentet er elegant, men det løser et koordineringsproblem mellom **uavhengige team** — som dere ikke har. For dere er det bare en unødvendig indirection som gir latens og gjør feilsøking vanskeligere.

---

## Hvor rapiden *faktisk* gir verdi

Den gir verdi for **de 9 eksterne appene** som også bruker `toi.rapid-1`:

| Ekstern app | Bruker rapiden til |
|---|---|
| rekrutteringsbistand-stilling-api | Publiserer stillingsendringer |
| foresporsel-om-deling-av-cv-api | Publiserer deling-hendelser |
| rekrutteringstreff-api | Ber om synlighetssjekk (`synlighetRekrutteringstreff`) |
| pam-dir-api | Leser stillinger |
| rekrutteringsbistand-kandidat-api | Publiserer kandidatliste-hendelser |

Her gjør rapiden nytte — den lar **andre repoer/team** kommunisere med kandidat-pipeline uten tett kobling.

---

## Konklusjon

| Bruksområde | Verdi av delt rapid |
|---|---|
| Intern kommunikasjon mellom de 24 appene | ❌ Unødvendig overhead — direkte kall er bedre |
| Ekstern kommunikasjon med andre repoers apper | ✅ Riktig mønster — løs kobling mellom team |
| Behovskjeden for beriking | ⚠️ Elegant, men løser et problem dere ikke har internt |

**Anbefaling:** Behold rapiden som **eksternt grensesnitt** (for de 9 appene utenfor repoet). Erstatt intern bruk med direkte funksjonskall i en konsolidert app. Da får dere det beste fra begge verdener: løs kobling eksternt, enkel og rask prosessering internt.
