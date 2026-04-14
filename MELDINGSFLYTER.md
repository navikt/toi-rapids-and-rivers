# Meldingsflyter til kandidat-indeksering

Dokumentasjon over alle meldingsflyter på rapiden som ender med kandidat-indeksering i OpenSearch.

## Oversikt

Systemet bruker [rapids-and-rivers](https://github.com/navikt/rapids-and-rivers)-arkitekturen. Alle apper deler én Kafka-topic (rapiden). Meldinger flyter gjennom rapiden og berikes underveis av ulike apper. Flyten starter når data kommer inn fra eksterne Kafka-topics, og ender når en komplett kandidatprofil indekseres i OpenSearch.

### KAFKA_EXTRA_TOPIC-mekanismen

Rapids-and-rivers-biblioteket støtter en `KAFKA_EXTRA_TOPIC`-konfigurasjon (satt i `nais.yaml`). Når denne er satt, abonnerer appens consumer på **både** rapid-topicen (`toi.rapid-1`) og det ekstra topicet. Meldinger fra det ekstra topicet dukker opp i consumeren uten `@event_name`-felt, noe transformator-appene utnytter med `forbid("@event_name")` for å kun plukke opp rå-meldinger fra det ekstra topicet.

| App | KAFKA_EXTRA_TOPIC | Ekstern kilde |
|-----|-------------------|---------------|
| toi-kvp | `pto.kvp-perioder-v1` | KVP-perioder fra PTO |
| toi-oppfolgingsinformasjon | `pto.endring-paa-oppfolgingsbruker-v2` | Oppfølgingsbrukerendringer fra PTO |
| toi-veileder | `pto.siste-tilordnet-veileder-v1` | Veiledertilordninger fra PTO |
| toi-siste-14a-vedtak | `pto.siste-14a-vedtak-v1` | 14a-vedtak fra PTO |
| toi-siste-oppfolgingsperiode-pond | `toi.siste-oppfolgingsperiode-fra-aktorid-v1` | Re-keyet oppfølgingsperioder fra toi-siste-oppfolgingsperiode |

### Roller i arkitekturen

| Rolle | Beskrivelse | Apper |
|-------|-------------|-------|
| **Inngangsporter** | Lytter på eksterne Kafka-topics (egne consumere) og publiserer hendelser til rapiden | toi-arbeidsmarked-cv, toi-arbeidssoekerperiode, toi-livshendelse |
| **Transformatorer** | Lytter på rå-meldinger fra `KAFKA_EXTRA_TOPIC` (uten `@event_name`) og transformerer til navngitte hendelser på rapiden | toi-kvp, toi-oppfolgingsinformasjon, toi-veileder, toi-siste-14a-vedtak, toi-siste-oppfolgingsperiode-pond |
| **Identmapper** | Slår opp aktørId fra fødselsnummer via PDL og beriker meldingen | toi-identmapper |
| **Aggregator** | Samler kandidatdata fra alle kilder i en database og svarer på behov | toi-sammenstille-kandidat |
| **Synlighetsmotor** | Evaluerer om en kandidat skal være synlig i søk | toi-synlighetsmotor |
| **Berikere** | Svarer på `@behov` med tilleggsdata fra eksterne tjenester | toi-organisasjonsenhet, toi-hull-i-cv, toi-ontologitjeneste, toi-geografi, toi-arbeidssoekeropplysninger, toi-siste-oppfolgingsperiode-pond, toi-livshendelse |
| **Indekserer** | Indekserer ferdig berikede kandidater i OpenSearch | toi-kandidat-indekser |

---

## Komplett flytdiagram

```mermaid
flowchart TD
    subgraph Eksterne Kafka-topics
        CV_TOPIC["teampam.cv-endret-ekstern-v2"]
        ASR_TOPIC["paw.arbeidssokerperioder-v1"]
        PDL_TOPIC["pdl.leesah-v1"]
        POAO_TOPIC["poao.siste-oppfolgingsperiode-v3"]
        KVP_TOPIC["pto.kvp-perioder-v1<br/>(KAFKA_EXTRA_TOPIC)"]
        OPP_TOPIC["pto.endring-paa-oppfolgingsbruker-v2<br/>(KAFKA_EXTRA_TOPIC)"]
        VEIL_TOPIC["pto.siste-tilordnet-veileder-v1<br/>(KAFKA_EXTRA_TOPIC)"]
        V14A_TOPIC["pto.siste-14a-vedtak-v1<br/>(KAFKA_EXTRA_TOPIC)"]
    end

    subgraph Inngangsporter
        toi-arbeidsmarked-cv
        toi-arbeidssoekerperiode
        toi-livshendelse
        toi-siste-oppfolgingsperiode["toi-siste-oppfolgingsperiode<br/>(Kafka Streams)"]
    end

    subgraph Transformatorer
        toi-kvp
        toi-oppfolgingsinformasjon
        toi-veileder
        toi-siste-14a-vedtak
    end

    subgraph Identmapper
        toi-identmapper
    end

    subgraph Aggregator
        toi-sammenstille-kandidat
    end

    subgraph Synlighetsberegning
        toi-synlighetsmotor
    end

    subgraph Berikere
        toi-organisasjonsenhet
        toi-hull-i-cv
        toi-ontologitjeneste
        toi-geografi
        toi-arbeidssoekeropplysninger
        toi-siste-oppfolgingsperiode-pond
        toi-livshendelse-behov["toi-livshendelse<br/>(AdressebeskyttelseLytter)"]
    end

    subgraph Indeksering
        toi-kandidat-indekser-uferdig["toi-kandidat-indekser<br/>(UferdigKandidatLytter)"]
        toi-kandidat-indekser-synlig["toi-kandidat-indekser<br/>(SynligKandidatfeedLytter)"]
        toi-kandidat-indekser-usynlig["toi-kandidat-indekser<br/>(UsynligKandidatfeedLytter)"]
        OPENSEARCH[(OpenSearch<br/>kandidater)]
    end

    %% Eksterne topics → Inngangsporter
    CV_TOPIC --> toi-arbeidsmarked-cv
    ASR_TOPIC --> toi-arbeidssoekerperiode
    PDL_TOPIC --> toi-livshendelse
    POAO_TOPIC --> toi-siste-oppfolgingsperiode

    %% KAFKA_EXTRA_TOPIC → Transformatorer
    KVP_TOPIC -.->|"KAFKA_EXTRA_TOPIC<br/>uten @event_name"| toi-kvp
    OPP_TOPIC -.->|"KAFKA_EXTRA_TOPIC<br/>uten @event_name"| toi-oppfolgingsinformasjon
    VEIL_TOPIC -.->|"KAFKA_EXTRA_TOPIC<br/>uten @event_name"| toi-veileder
    V14A_TOPIC -.->|"KAFKA_EXTRA_TOPIC<br/>uten @event_name"| toi-siste-14a-vedtak

    %% Inngangsporter → hendelser med @event_name
    toi-arbeidsmarked-cv -->|"arbeidsmarked-cv"| toi-sammenstille-kandidat
    toi-arbeidssoekerperiode -->|"arbeidssokerperiode"| toi-identmapper
    toi-livshendelse -->|"adressebeskyttelse"| toi-synlighetsmotor

    %% Identmapper beriker med aktørId
    toi-identmapper -->|"arbeidssokerperiode<br/>+ aktørId"| toi-arbeidssoekeropplysninger
    toi-identmapper -->|"oppfølgingsinformasjon<br/>+ aktørId"| toi-sammenstille-kandidat

    %% Arbeidssøkeropplysninger lagrer og publiserer
    toi-arbeidssoekeropplysninger -->|"arbeidssokeropplysninger"| toi-synlighetsmotor

    %% Transformatorer → hendelser med @event_name
    toi-kvp -->|"kvp"| toi-sammenstille-kandidat
    toi-oppfolgingsinformasjon -->|"oppfølgingsinformasjon"| toi-identmapper
    toi-veileder -->|"veileder"| toi-sammenstille-kandidat
    toi-siste-14a-vedtak -->|"siste14avedtak"| toi-sammenstille-kandidat
    toi-siste-oppfolgingsperiode -->|"toi.siste-oppfolgingsperiode<br/>-fra-aktorid-v1"| toi-siste-oppfolgingsperiode-pond

    %% Synlighetsmotor legger til @behov for manglende data
    toi-synlighetsmotor -->|"+ @behov"| toi-sammenstille-kandidat

    %% Sammenstille-kandidat svarer på @behov
    toi-sammenstille-kandidat -->|"besvart behov"| toi-synlighetsmotor
    toi-sammenstille-kandidat -->|"besvart behov"| toi-kandidat-indekser-uferdig

    %% Synlighetsmotor beregner synlighet
    toi-synlighetsmotor -->|"+ synlighet"| toi-kandidat-indekser-uferdig
    toi-synlighetsmotor -->|"erSynlig=false"| toi-kandidat-indekser-usynlig

    %% UferdigKandidatLytter legger til @behov for berikere
    toi-kandidat-indekser-uferdig -->|"@behov: organisasjonsenhetsnavn,<br/>hullICv, ontologi, geografi"| toi-organisasjonsenhet
    toi-organisasjonsenhet -->|"+ organisasjonsenhetsnavn"| toi-hull-i-cv
    toi-hull-i-cv -->|"+ hullICv"| toi-ontologitjeneste
    toi-ontologitjeneste -->|"+ ontologi"| toi-geografi
    toi-geografi -->|"+ geografi"| toi-kandidat-indekser-synlig

    %% Indeksering
    toi-kandidat-indekser-synlig -->|"indekser CV"| OPENSEARCH
    toi-kandidat-indekser-usynlig -->|"slett CV"| OPENSEARCH
```

---

## Detaljerte flyter per inngangsport

### Flyt 1: CV-endring (arbeidsmarked-cv)

Trigges av endringer i en kandidats CV fra Arbeidsmarked.

```mermaid
sequenceDiagram
    participant EKS as teampam.cv-endret-ekstern-v2
    participant CV as toi-arbeidsmarked-cv
    participant SAM as toi-sammenstille-kandidat
    participant SYN as toi-synlighetsmotor
    participant SAM2 as toi-sammenstille-kandidat<br/>(NeedLytter)
    participant UFERDIG as toi-kandidat-indekser<br/>(UferdigKandidatLytter)
    participant ORG as toi-organisasjonsenhet
    participant HULL as toi-hull-i-cv
    participant ONT as toi-ontologitjeneste
    participant GEO as toi-geografi
    participant SYNLIG as toi-kandidat-indekser<br/>(SynligKandidatfeedLytter)
    participant OS as OpenSearch

    EKS->>CV: Avro-melding (CV-endring)
    CV->>CV: Konverterer Avro → JSON
    CV-->>SAM: @event_name=arbeidsmarked-cv<br/>aktørId, arbeidsmarkedCv
    SAM->>SAM: Lagrer arbeidsmarkedCv i DB

    Note over SYN: Plukker opp melding med<br/>arbeidsmarkedCv-feltet
    CV-->>SYN: Samme melding plukkes opp
    SYN->>SYN: Evaluerer synlighet
    alt Mangler data for synlighetsberegning
        SYN-->>SAM2: Legger til @behov for manglende felter
        SAM2->>SAM2: Henter kandidat fra DB
        SAM2-->>SYN: Svarer med berikede felter
        SYN->>SYN: Re-evaluerer synlighet
    end
    alt Synlig kandidat
        SYN-->>UFERDIG: synlighet.erSynlig=true,<br/>synlighet.ferdigBeregnet=true
        UFERDIG->>UFERDIG: Legger til @behov:<br/>organisasjonsenhetsnavn, hullICv,<br/>ontologi, geografi
        UFERDIG-->>ORG: @behov[0]=organisasjonsenhetsnavn
        ORG->>ORG: Slår opp NAV-kontornavn i NORG2
        ORG-->>HULL: + organisasjonsenhetsnavn
        HULL->>HULL: Beregner hull i CV
        HULL-->>ONT: + hullICv
        ONT->>ONT: Slår opp synonymer for<br/>kompetanser og stillingstitler
        ONT-->>GEO: + ontologi
        GEO->>GEO: Slår opp geografi fra<br/>postnummer og geografikoder
        GEO-->>SYNLIG: + geografi (alle behov løst)
        SYNLIG->>OS: Indekserer kandidat-CV
        SYNLIG-->>SYNLIG: @slutt_av_hendelseskjede=true
    end
    alt Usynlig kandidat
        SYN-->>OS: synlighet.erSynlig=false → Slett fra indeks
    end
```

### Flyt 2: KVP-hendelse

Trigges når en bruker starter eller avslutter KVP (Kvalifiseringsprogrammet).

```mermaid
sequenceDiagram
    participant EKS as pto.kvp-perioder-v1<br/>(KAFKA_EXTRA_TOPIC)
    participant KVP as toi-kvp
    participant SAM as toi-sammenstille-kandidat<br/>(SamleLytter)
    participant SYN as toi-synlighetsmotor
    participant SAM2 as toi-sammenstille-kandidat<br/>(NeedLytter)
    participant IND as toi-kandidat-indekser

    Note over EKS: Rå-melding via<br/>KAFKA_EXTRA_TOPIC<br/>uten @event_name,<br/>med event, aktorId,<br/>startet, enhetId
    EKS-->>KVP: Rå kvp-melding
    KVP->>KVP: Validerer event=STARTET/AVSLUTTET
    KVP-->>SAM: @event_name=kvp<br/>aktørId, kvp{event, startet, avsluttet, enhetId}
    SAM->>SAM: Lagrer kvp i DB
    Note over SYN: Plukker også opp meldingen<br/>(har minst ett relevant felt)
    KVP-->>SYN: Samme melding plukkes opp
    SYN->>SYN: Evaluerer synlighet
    SYN-->>SAM2: @behov for manglende felter
    SAM2-->>SYN: Berikede felter fra DB
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1<br/>(berikere → indeksering)
```

### Flyt 3: Oppfølgingsinformasjon

Trigges når oppfølgingsinformasjon endres for en bruker.

```mermaid
sequenceDiagram
    participant EKS as pto.endring-paa-oppfolgingsbruker-v2<br/>(KAFKA_EXTRA_TOPIC)
    participant OPP as toi-oppfolgingsinformasjon
    participant IDM as toi-identmapper
    participant SAM as toi-sammenstille-kandidat
    participant SYN as toi-synlighetsmotor
    participant IND as toi-kandidat-indekser

    Note over EKS: Rå-melding via<br/>KAFKA_EXTRA_TOPIC<br/>uten @event_name,<br/>med fodselsnummer,<br/>oppfolgingsenhet,<br/>harOppfolgingssak
    EKS-->>OPP: Rå oppfølgingsmelding
    OPP-->>IDM: @event_name=oppfølgingsinformasjon<br/>fodselsnummer, oppfølgingsinformasjon
    Note over IDM: Slår opp aktørId fra<br/>fødselsnummer via PDL
    IDM-->>SAM: + aktørId
    SAM->>SAM: Lagrer oppfølgingsinformasjon i DB
    IDM-->>SYN: Samme melding plukkes opp
    SYN->>SYN: Evaluerer synlighet
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1
```

### Flyt 4: Veiledertilordning

Trigges når en veileder tilordnes en bruker.

```mermaid
sequenceDiagram
    participant EKS as pto.siste-tilordnet-veileder-v1<br/>(KAFKA_EXTRA_TOPIC)
    participant VEIL as toi-veileder
    participant SAM as toi-sammenstille-kandidat
    participant SYN as toi-synlighetsmotor
    participant IND as toi-kandidat-indekser

    Note over EKS: Rå-melding via<br/>KAFKA_EXTRA_TOPIC<br/>uten @event_name,<br/>med aktorId, veilederId
    EKS-->>VEIL: Rå veiledermelding
    VEIL->>VEIL: Slår opp veilederinfo i NOM
    VEIL-->>SAM: @event_name=veileder<br/>aktørId, veileder{aktorId, veilederId, veilederinformasjon}
    SAM->>SAM: Lagrer veileder i DB
    VEIL-->>SYN: Samme melding plukkes opp
    SYN->>SYN: Evaluerer synlighet
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1
```

### Flyt 5: Siste 14a-vedtak

Trigges når et nytt 14a-vedtak fattes.

```mermaid
sequenceDiagram
    participant EKS as pto.siste-14a-vedtak-v1<br/>(KAFKA_EXTRA_TOPIC)
    participant V14 as toi-siste-14a-vedtak
    participant SAM as toi-sammenstille-kandidat
    participant SYN as toi-synlighetsmotor
    participant IND as toi-kandidat-indekser

    Note over EKS: Rå-melding via<br/>KAFKA_EXTRA_TOPIC<br/>uten @event_name,<br/>med aktorId, innsatsgruppe,<br/>hovedmal, fattetDato
    EKS-->>V14: Rå 14a-vedtak-melding
    V14-->>SAM: @event_name=siste14avedtak<br/>aktørId, siste14avedtak
    SAM->>SAM: Lagrer siste14avedtak i DB
    V14-->>SYN: Samme melding plukkes opp
    SYN->>SYN: Evaluerer synlighet
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1
```

### Flyt 6: Arbeidssøkerperiode

Trigges fra arbeidssøkerregisteret.

```mermaid
sequenceDiagram
    participant EKS as paw.arbeidssokerperioder-v1
    participant ASP as toi-arbeidssoekerperiode
    participant IDM as toi-identmapper
    participant AOP as toi-arbeidssoekeropplysninger<br/>(ArbeidssoekerperiodeRapidLytter)
    participant AOP2 as toi-arbeidssoekeropplysninger<br/>(PubliserOpplysningerJobb)
    participant SYN as toi-synlighetsmotor
    participant IND as toi-kandidat-indekser

    EKS->>ASP: Avro Periode-melding
    ASP-->>IDM: @event_name=arbeidssokerperiode<br/>fodselsnummer, arbeidssokerperiode
    IDM->>IDM: Slår opp aktørId fra<br/>fødselsnummer via PDL
    IDM-->>AOP: + aktørId
    AOP->>AOP: Lagrer arbeidssøkerperiode i DB
    Note over AOP2: Periodisk jobb som<br/>publiserer ubehandlede<br/>opplysninger
    AOP2-->>SYN: @event_name=arbeidssokeropplysninger<br/>aktørId, arbeidssokeropplysninger
    SYN->>SYN: Evaluerer synlighet
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1
```

### Flyt 7: Oppfølgingsperiode

Trigges fra oppfølgingsperiode-systemet.

```mermaid
sequenceDiagram
    participant EKS as poao.siste-oppfolgingsperiode-v3
    participant SOP as toi-siste-oppfolgingsperiode<br/>(Kafka Streams)
    participant TOI_TOPIC as toi.siste-oppfolgingsperiode<br/>-fra-aktorid-v1
    participant SOPOND as toi-siste-oppfolgingsperiode-pond<br/>(SisteOppfolgingsperiodeLytter)
    participant SYN as toi-synlighetsmotor
    participant IND as toi-kandidat-indekser

    EKS->>SOP: Oppfølgingsperiode-melding
    SOP->>SOP: Re-keyer til aktørId,<br/>holder siste periode per aktør
    SOP->>TOI_TOPIC: Siste oppfølgingsperiode
    TOI_TOPIC->>SOPOND: Rå melding uten @event_name
    SOPOND-->>SYN: @event_name=sisteOppfølgingsperiode<br/>aktørId, sisteOppfølgingsperiode
    SYN->>SYN: Evaluerer synlighet
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1
```

### Flyt 8: Adressebeskyttelse (livshendelse)

Trigges av adressebeskyttelse-hendelser fra PDL.

```mermaid
sequenceDiagram
    participant EKS as pdl.leesah-v1
    participant LIV as toi-livshendelse<br/>(PDLLytter)
    participant SYN as toi-synlighetsmotor
    participant IND as toi-kandidat-indekser

    EKS->>LIV: Personhendelse (Avro)
    LIV->>LIV: Filtrerer ADRESSEBESKYTTELSE_<br/>Slår opp gradering i PDL
    LIV-->>SYN: @event_name=adressebeskyttelse<br/>aktørId, adressebeskyttelse (gradering)
    SYN->>SYN: Evaluerer synlighet
    SYN-->>IND: synlighet beregnet
    Note over IND: Videre flyt som i Flyt 1
```

---

## Behovsmekanismen (@behov)

Meldinger berikes stegvis gjennom et behovsmønster. Når en app mangler data, legger den til felter i `@behov`-arrayet. Berikere plukker opp meldingen når deres behov er det **første uløste** i listen.

```mermaid
flowchart LR
    subgraph Synlighetsmotor behov
        S1["@behov: [arbeidsmarkedCv,<br/>veileder, oppfølgingsinformasjon,<br/>siste14avedtak, sisteOppfølgingsperiode,<br/>kvp, arbeidssokeropplysninger]"]
        S2["Eventuelt: + adressebeskyttelse"]
    end

    subgraph Kandidat-indekser behov
        K1["@behov: [..., organisasjonsenhetsnavn,<br/>hullICv, ontologi, geografi]"]
    end

    S1 -->|"toi-sammenstille-kandidat<br/>svarer sekvensielt"| S2
    S2 -->|"toi-livshendelse<br/>svarer"| K1
    K1 -->|"Berikere svarer<br/>sekvensielt"| FERDIG[Alle behov løst]
```

### Behovskjeden for berikere (kandidat-indekser)

Berikerne svarer i rekkefølge basert på det første uløste behovet:

```mermaid
flowchart LR
    A["@behov: organisasjonsenhetsnavn,<br/>hullICv, ontologi, geografi"]
    B["toi-organisasjonsenhet<br/>→ + organisasjonsenhetsnavn"]
    C["toi-hull-i-cv<br/>→ + hullICv"]
    D["toi-ontologitjeneste<br/>→ + ontologi"]
    E["toi-geografi<br/>→ + geografi"]
    F["Alle behov løst ✓"]

    A --> B --> C --> D --> E --> F
```

---

## Synlighetsregler

`toi-synlighetsmotor` evaluerer følgende kriterier for at en kandidat skal være synlig:

| Kriterium | Datakilde | Regel |
|-----------|-----------|-------|
| Har aktiv CV | arbeidsmarkedCv | meldingstype er OPPRETT eller ENDRE |
| Har oppfølging | sisteOppfølgingsperiode | startdato er i fortid OG sluttdato er null eller i fremtid |
| Riktig formidlingsgruppe | oppfølgingsinformasjon | formidlingsgruppe == ARBS |
| Ikke kode 6/7 | oppfølgingsinformasjon | diskresjonskode ikke er "6" eller "7" |
| Ikke sperret ansatt | oppfølgingsinformasjon | sperretAnsatt == false |
| Ikke død | oppfølgingsinformasjon | erDoed == false |
| Ikke i KVP | kvp | event != STARTET |
| Ingen adressebeskyttelse | adressebeskyttelse | gradering er UGRADERT eller UKJENT |
| Er arbeidssøker | arbeidssokeropplysninger | periodeStartet != null OG periodeAvsluttet == null |

Alle kriterier må være oppfylt for at kandidaten skal være synlig (`erSynlig=true`).

---

## Meldinger og @event_name-oversikt

| @event_name | Produsent | Konsumenter |
|-------------|-----------|-------------|
| `arbeidsmarked-cv` | toi-arbeidsmarked-cv | toi-sammenstille-kandidat (SamleLytter) |
| `kvp` | toi-kvp | toi-sammenstille-kandidat (SamleLytter) |
| `oppfølgingsinformasjon` | toi-oppfolgingsinformasjon | toi-identmapper → toi-sammenstille-kandidat |
| `veileder` | toi-veileder | toi-sammenstille-kandidat (SamleLytter) |
| `siste14avedtak` | toi-siste-14a-vedtak | toi-sammenstille-kandidat (SamleLytter) |
| `arbeidssokerperiode` | toi-arbeidssoekerperiode | toi-identmapper → toi-arbeidssoekeropplysninger |
| `arbeidssokeropplysninger` | toi-arbeidssoekeropplysninger | toi-synlighetsmotor |
| `sisteOppfølgingsperiode` | toi-siste-oppfolgingsperiode-pond | toi-synlighetsmotor |
| `adressebeskyttelse` | toi-livshendelse | toi-synlighetsmotor |
| `republisert` | toi-sammenstille-kandidat (manuell) | Alle lyttere (starter full re-flyt) |

---

## Applikasjoner som IKKE inngår i kandidat-indeksering

Følgende apper i repoet har andre formål og er ikke del av flyten til kandidat-indeksering:

- **toi-arbeidsgiver-notifikasjon** – Håndterer notifikasjoner til arbeidsgivere
- **toi-evaluertdatalogger** – Logger data fra republiserte meldinger
- **toi-helseapp** – Helsesjekk og monitorering av rapiden
- **toi-stilling-indekser** – Indekserer stillinger (separat flyt)
- **toi-publiser-dir-stillinger** – Publiserer direktemeldte stillinger
- **toi-publisering-til-arbeidsplassen** – Publiserer stillinger til Arbeidsplassen
