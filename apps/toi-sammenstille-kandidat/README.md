# Sammenstiller kandidatinformasjon

## Henter ut cv og veilederinformasjon, lagrer i database, og legger sammenstillt kandidatinformasjon tilbake på rapid

### Hvordan koble seg til MongoDB
* List opp alle pod'er:

    kubectl get pods -n=toi
* Gå inn en av mongodb-pod'ene:
  
    kubectl exec -it toi-sammenstille-kandidat-mongodb-0 /bin/bash -n=toi

* Start MongoShell:

    mongosh

* Se MongoShells dokumentasjon for kommandoer: https://docs.mongodb.com/mongodb-shell/run-commands/


