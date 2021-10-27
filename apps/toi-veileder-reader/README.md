# Henter ut veilederinformasjon og legger den på rapid

## TODO: Skal bytte ut tidligere løsning
Denne informasjonen har tidligere kommet fra Arbeidsplassen topic, under Oppfølgingsiformasjon, felt veileder.
Formatet er "X000000".
Vi skal få denne informasjonen fra nytt topic, "siste-tilordnet-veileder-v1"
Etter at dette er på rapid, må vi ha en ny rapid konsument som sammenstiller denne informasjonen med dataene fra arbeidsplassen cv tiopic, 
og oversender data til indekser. Indekser må kunne lytte på nytt format, og tolke det riktig med veildederinformasjonen.

Format og topic:
// topic (både dev og prod): siste-tilordnet-veileder-v1
public class SisteTilordnetVeilederKafkaDTO {
String aktorId;
String veilederId;
ZonedDateTime tilordnet;
}