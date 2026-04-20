# Plan: Utvide toi-identmapper med toveis ident-oppslag

Utvid `toi-identmapper` fra enveis cache (fnr → aktørId) til et toveis ident-oppslag som også støtter aktørId → fnr, og la tjenesten holde seg selv oppdatert via PDL-topic i stedet for REST. Eksisterende `identmapping`-tabell gjenbrukes uten skjemaendringer. Konsumenter (i første omgang `toi-synlighetsmotor` og `toi-sammenstille-kandidat`) får fnr beriket i meldingene sine uten egen DB-kobling eller PDL-kobling.

I denne planen navngis komponentene symmetrisk for begge retninger:
- `Repository` → **`IdentRepository`**
- `AktorIdCache` → **`IdentCache`** (dekker både fnr→aktørId og aktørId→fnr)
- Eksisterende `Lytter` → **`FødselsnummerLytter`** (fnr inn, aktørId ut)
- Ny rapid-lytter → **`AktørIdLytter`** (aktørId inn, fnr ut)
- Ny Kafka-konsument → **`PdlAktorLytter`** (PDL-topic → DB-upsert)

## Steg

1. **Utvid [PdlKlient.kt](apps/toi-identmapper/src/main/kotlin/no/nav/arbeidsgiver/toi/identmapper/PdlKlient.kt)** midlertidig med `hentFødselsnummer(aktørId: String): String?` (brukes i PR 2–4 inntil PDL-topic-konsumenten i PR 5 overtar). Spør PDL med `grupper: [FOLKEREGISTERIDENT]`, `historikk: false`. REST-klienten fjernes i PR 5.

2. **Rename `Repository.kt` → `IdentRepository.kt`** (klassenavn `IdentRepository`) og utvid — ingen migrering/datamodellendring. `identmapping(aktor_id, fnr, cachet_tidspunkt)` gjenbrukes.
   - Ny metode `hentIdentMappingerForAktørId(aktørId: String): List<IdentMapping>` (analogt med eksisterende `hentIdentMappinger(fnr)`).
   - Ny metode `lagreFødselsnummer(aktørId: String, fødselsnummer: String?)` som upserter på `(aktor_id, fnr)`: finnes raden allerede → oppdater `cachet_tidspunkt`, ellers insert. Gjenbruker unique constraint `uq_fnr_aktor_id`.
   - Generaliser gjerne eksisterende `lagreAktørId` og den nye til én felles privat upsert-funksjon siden logikken blir symmetrisk.
   - Oppdater alle referanser i [Application.kt](apps/toi-identmapper/src/main/kotlin/no/nav/arbeidsgiver/toi/identmapper/Application.kt) og eksisterende tester.

3. **Rename `AktorIdCache.kt` → `IdentCache.kt`** (klassenavn `IdentCache`) og utvid slik at den eksponerer både `hentAktørId(fødselsnummer: String): String?` og `hentFødselsnummer(aktørId: String): String?` med samme cache-først-så-PDL-mønster. Bevar negativ-caching-flagget (`cacheNårAktørIdErNull`) — vurder å gjøre det symmetrisk for begge retninger. Etter PR 5 blir dette et rent DB-oppslag (topic-konsumenten sørger for dekning).

4. **Rename eksisterende `Lytter.kt` → `FødselsnummerLytter.kt`** (klassenavn `FødselsnummerLytter`) for å gjøre det tydelig hva den håndterer. Oppførselen er uendret: fnr inn → beriker med `aktørId`. Oppdater instansiering i `Application.kt`.

5. **Ny `AktørIdLytter` i `AktørIdLytter.kt`** som plukker meldinger med `aktørId` men uten fnr, slår opp via `IdentCache.hentFødselsnummer`, og beriker meldingen med `fodselsnummer` før republisering. Registreres fra `Application.kt`.

6. **Whitelist på rapid-meldinger** — i første omgang kun synlighetsmeldinger fra `toi-synlighetsmotor`:
   - Implementer som en precondition i `AktørIdLytter`, f.eks. `requireKey("synlighet")` (eller annet distinkt felt, se Videre vurderinger), slik at kun synlighetsmeldinger berikes.
   - Gjør whitelist-kriteriet konfigurerbart/utvidbart (liste av required-felt) så nye meldingstyper kan legges til uten kodeendring i lytteren.

7. **Tilpass [SynlighetsgrunnlagLytter.kt](apps/toi-synlighetsmotor/src/main/kotlin/no/nav/arbeidsgiver/toi/SynlighetsgrunnlagLytter.kt)** slik at synlighetsmeldingen publiseres selv om fnr mangler — identmapperen vil da ta seg av berikelsen i neste hopp. Dette erstatter behovet for direkte DB-fallback i synlighetsmotor.

8. **Oppdater tester** i `toi-identmapper/src/test`:
   - Repository-test for den nye upserten (insert + oppdatert `cachet_tidspunkt`, ingen duplikater).
   - Repository-test for oppslag via aktørId.
   - Lytter-test som verifiserer at synlighetsmelding uten fnr men med aktørId blir beriket, og at andre meldingstyper ignoreres av whitelist.
   - PDL-mock for `hentFødselsnummer`.

9. **Ny PDL-topic-konsument `PdlAktorLytter` og utfasing av PDL REST-klienten.** Identmapper har allerede avro-skjemaet [AktorV2.avdl](apps/toi-identmapper/src/main/avro/AktorV2.avdl) (namespace `no.nav.person.pdl.aktor.v2`) liggende ubrukt.
   - Konsumer topic `pdl-aktor-v2` med Avro/Confluent-deserializer (samme mønster som [PDLLytter.kt](apps/toi-livshendelse/src/main/kotlin/no/nav/arbeidsgiver/toi/livshendelser/PDLLytter.kt) i `toi-livshendelse`, som allerede konsumerer `pdl.leesah-v1`).
   - For hver `Aktor`-hendelse: plukk `gjeldende` `FOLKEREGISTERIDENT` og `AKTORID` og upsert via `IdentRepository`. Håndter tombstones (slettede aktører) eksplisitt.
   - Slett [PdlKlient.kt](apps/toi-identmapper/src/main/kotlin/no/nav/arbeidsgiver/toi/identmapper/PdlKlient.kt) og [AccessTokenClient.kt](apps/toi-identmapper/src/main/kotlin/no/nav/arbeidsgiver/toi/identmapper/AccessTokenClient.kt). `IdentCache` forenkles til rent DB-oppslag (null hvis miss), siden topic-konsumenten sørger for dekning.
   - NAIS-manifest ([nais-dev.yaml](apps/toi-identmapper/nais-dev.yaml) / [nais-prod.yaml](apps/toi-identmapper/nais-prod.yaml)): fjern `PDL_URL`/`PDL_SCOPE`/Azure-PDL-scope, legg til Kafka-tilgang mot `pdl-aktor-v2` og schema-registry-credentials.
   - Tester: mapping `Aktor` → upsert, at ikke-gjeldende identifikatorer ignoreres, tombstone-håndtering.

## Relatert problemstilling: `/evaluering/{fnr}` i synlighetsmotor

`toi-synlighetsmotor` eksponerer et HTTP-endepunkt `/evaluering/{fnr}` (brukes bl.a. av Rekrutteringsbistand-søk). R&R-backend lagrer primært på aktørId, og fnr-kolonnen i `evaluering`-tabellen kan være `null` når input manglet fnr. Det gir tomme treff selv om dataene finnes på aktørId.

Stegene over løser *overføringen* av fnr i rapid-meldinger, men løser ikke automatisk HTTP-API-søket. To alternativer bør vurderes (helst som egen PR 6):

- **Alternativ A (minst endring):** I `EvalueringController` oversett fnr → aktørId via identmapper (delt DB, intern HTTP, eller nytt rapid-behov) og søk på aktørId i synlighetsmotor-DB. Fnr-kolonnen i `evaluering` kan beholdes som cache/backup eller fjernes.
- **Alternativ B (grundigere):** Dropp fnr-kolonnen i `evaluering` helt; gjør aktørId til eneste søkenøkkel. Rekrutteringsbistand (og andre konsumenter av API-et) oversetter fnr → aktørId før kall.

Anbefaler **Alternativ A** som neste steg fordi det er bakoverkompatibelt og kan rulles ut uten samtidige endringer hos konsumenter.

## Videre vurderinger

1. **Hvilket felt identifiserer en synlighetsmelding unikt i whitelisten?** `synlighet` (settes av `SynlighetsgrunnlagLytter` før publisering) er den mest presise markøren. Må bekreftes at den alltid er til stede på disse meldingene og ikke brukes andre steder.
2. **Rekkefølge og idempotens ved race:** Hvis en melding publiseres uten fnr, og identmapper senere beriker — sørg for at downstream-konsumenter tåler at samme aktørId-melding kan komme i to varianter (uten og med fnr). Alternativt: ikke publiser fra synlighetsmotor før identmapper har svart (men det bryter rapid-mønsteret).
3. **Inkonsistent data ved PDL-endringer:** Løses strukturelt av PR 5 (topic-oppdateringer holder `identmapping` ferskt). Ha observability på lag mellom topic og DB slik at stopp i konsumenten oppdages raskt.
4. **Backfill av eksisterende rader:** PR 5 dekker fremtidige endringer, men historiske aktørId-er fylles først ved neste PDL-hendelse eller ved eksplisitt oppslag. Vurder engangs-backfill (les `aktor_id`-er fra synlighetsmotor-DB, kall PDL REST én siste gang, upsert i identmapper) *før* REST-klienten slettes i PR 5.
5. **Sammenstilleren (`toi-sammenstille-kandidat`):** Nyter samme berikelse så snart meldingene har fnr. Ingen direkte endring nødvendig, men verifiser at den håndterer meldinger uten fnr på en forutsigbar måte i mellomtiden.

## Foreslått PR-oppdeling

Arbeidet deles i seks mindre PR-er som hver leverer verdi isolert, kan revieweres og deployes uavhengig, og minimerer risiko for regresjon. PR 1–3 og PR 5 er endringer i `toi-identmapper`; PR 4 og PR 6 tar i bruk funksjonaliteten fra hhv. `SynlighetsgrunnlagLytter` og `EvalueringController` i `toi-synlighetsmotor`.

### PR 1 — Rename og strukturell opprydding (ren refaktor, ingen funksjonsendring)
**Mål:** Etablere symmetrisk navngiving før funksjonell utvidelse, slik at de påfølgende diffene blir rene.
- Rename `Repository.kt` → `IdentRepository.kt` (klasse `Repository` → `IdentRepository`).
- Rename `AktorIdCache.kt` → `IdentCache.kt` (klasse `AktorIdCache` → `IdentCache`).
- Rename `Lytter.kt` → `FødselsnummerLytter.kt` (klasse `Lytter` → `FødselsnummerLytter`).
- Oppdater alle instansieringer i `Application.kt` og eksisterende tester.
- Ingen logikk- eller API-endring; eksisterende tester skal passere uendret.
**Risiko:** Lav. Anbefales som første PR for å holde senere diff fokusert.

### PR 2 — Toveis oppslag i datalag (Repository + Cache + PDL), ingen ny lytter
**Mål:** Tilby `aktørId → fnr`-funksjonalitet internt, men uten å eksponere den på rapid ennå.
- Utvid `PdlKlient` med `hentFødselsnummer(aktørId)`.
- Utvid `IdentRepository` med `hentIdentMappingerForAktørId(...)` og `lagreFødselsnummer(...)` (upsert på eksisterende `identmapping`-tabell). Refaktorer til felles privat upsert-funksjon.
- Utvid `IdentCache` med `hentFødselsnummer(aktørId)` (cache-først-så-PDL), symmetrisk med eksisterende `hentAktørId`.
- Legg til enhetstester for nye metoder (Repository upsert, Cache-fallthrough til PDL, PDL-respons-parsing).
**Leverer:** Ingen synlig adferdsendring i prod. Merges trygt fordi ny kode er ubrukt, men dekkes av tester.
**Risiko:** Lav–middels. Feil i upsert kan påvirke eksisterende `lagreAktørId`-sti — avbøtes ved å beholde den gamle metodens signatur intakt og dekke felles kode med tester.

### PR 3 — `AktørIdLytter` med whitelist
**Mål:** Ta i bruk datalaget fra PR 2 og tilby berikelse på rapid, men kun for whitelistede meldinger.
- Ny `AktørIdLytter.kt` som leser `aktørId`, krever fravær av `fodselsnummer`/`fnr`/`fodselsnr`, og har precondition som matcher whitelisten.
- Whitelist implementert som konfigurerbar liste av required-felt (i PR 3 initialisert til `listOf("synlighet")`). Eksponer gjennom konstruktør så den er enkel å utvide.
- Registrering i `Application.kt`.
- Tester: beriker synlighetsmelding korrekt, ignorerer meldinger uten whitelist-match, ignorerer meldinger som allerede har fnr, håndterer miss i både cache og PDL (ingen publisering, logger i securelog).
**Leverer:** Identmapper vil nå berike synlighetsmeldinger uten fnr i prod, forutsatt at synlighetsmotor publiserer dem (skjer først i PR 4). Ingen skade hvis PR 4 forsinkes.
**Risiko:** Middels. Må verifiseres i dev at whitelist-precondition ikke plukker uønskede meldinger. Rulles ut først i dev-gcp.

### PR 4 — Synlighetsmotor publiserer uten fnr
**Mål:** La synlighetsmotor publisere meldinger selv når `kandidat.fødselsNummer()` er null, og la identmapperen ta seg av berikelsen.
- Endring i `SynlighetsgrunnlagLytter.kt`: fjern fnr-kravet før publisering. Dokumenter i kommentar at berikelse skjer i identmapper.
- Oppdater tester i `toi-synlighetsmotor` tilsvarende.
- Verifiser i dev-gcp end-to-end før prod-utrulling: melding fra synlighetsmotor uten fnr → identmapper beriker → downstream ser `fodselsnummer`.
**Leverer:** Lukker det opprinnelige edge-case-problemet (kandidater med kun adressebeskyttelse får fnr i utgående melding).
**Risiko:** Middels. Hvis PR 3 ikke er i prod, vil meldinger mangle fnr lenger nedstrøms. Deploy-rekkefølge: PR 3 til prod før PR 4.

### PR 5 — PDL-topic-konsument + fjerning av PDL REST-klient
**Mål:** Holde `identmapping` proaktivt oppdatert fra `pdl-aktor-v2`, fjerne REST-avhengigheten mot PDL, og løse stale-problemet strukturelt.
- Ny `PdlAktorLytter.kt` som konsumerer `pdl-aktor-v2` (Avro-skjemaet finnes allerede i `AktorV2.avdl`). Mapper `Aktor` → upsert av gjeldende fnr/aktørId.
- Håndter tombstone-meldinger (slettede aktører) — logg og ignorer, eller slett raden (avklares).
- Før merge: engangs-backfill (se Videre vurderinger #4) for aktørId-er vi allerede kjenner, slik at `IdentCache`-oppslag fortsatt treffer når REST-klienten forsvinner.
- Slett `PdlKlient.kt` og `AccessTokenClient.kt`. Forenkle `IdentCache` til rent DB-oppslag. Fjern PDL-konfig i NAIS-manifestet og legg til Kafka-tilgang + schema-registry-credentials.
- Observability: metric for antall hendelser konsumert og upserts, alarm ved stopp i konsumenten.
**Leverer:** Lavere infrastruktur-footprint (ingen PDL-tokens, ingen REST-retry), ferskere data, løser stale-problemet fra Videre vurderinger #3.
**Risiko:** Middels–høy. Gjøres alene, ikke sammenpakket med andre endringer. Rulles ut i dev-gcp med monitorering før prod.

### PR 6 — `/evaluering/{fnr}` slår opp via identmapper (Alternativ A)
**Mål:** Løse tomme treff i HTTP-søket når `evaluering`-raden mangler fnr.
- I `EvalueringController` (toi-synlighetsmotor): oversett fnr → aktørId via identmapper (delt DB eller intern HTTP) og slå opp på aktørId i synlighetsmotor-DB.
- Fjerne avhengighet av fnr-kolonnen i søk (kolonnen kan beholdes som cache eller fjernes i senere opprydding).
- Tester: søk med fnr som finnes i identmapping men ikke som kolonne i `evaluering` returnerer korrekt rad.
**Leverer:** Rekrutteringsbistand-søk på fnr finner evalueringer også for rader som ble lagret uten fnr.
**Risiko:** Lav–middels. Må sikre at identmapper-oppslag er raskt nok for HTTP-endepunktet (caching/indeks er på plass).

### Avhengighetsgraf
```
PR 1 (rename) ── PR 2 (datalag) ── PR 3 (AktørIdLytter) ── PR 4 (synlighetsmotor)
                                                        └── PR 5 (PDL-topic, fjern REST)
                                                                            └── PR 6 (/evaluering/{fnr})
```
PR 4 og PR 5 kan i prinsippet kjøre i parallell etter PR 3. PR 6 bør vente til PR 5 er i prod for best datakvalitet.

