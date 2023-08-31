package no.nav.arbeidsgiver.toi.veileder

private fun spørringForCvDeltMedArbeidsgiver(identer: List<String>) {

    val template = """
        query(${'$'}identer: [String!]!) {
          ressurser(where: { navidenter: ${'$'}identer }) {
            id
            ressurs {
              navIdent
              visningsNavn
              fornavn
              etternavn
              epost
            }
          }
        }
    """.trimIndent()

    val variabler = """
        {
          "identer": ${identer.joinToString(prefix = "[", separator = ",", )}
        }
    """.trimIndent()
}
