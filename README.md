# Tiltak og inkludering's rapids & rivers-microservicer

## Github packages-dependencies
For å kunn bygge kreves noen pakker fra github packages. Disse kan lastes ned ved å lage til en personal access token med read-packages-scope. 
Bygg-scriptet leter etter gradle-propertien `mavenUserGithub` som kan settes i gradle.properties i hjemme-området ditt.
Alternativt letes det etter system-variabelen GITHUB_TOKEN.