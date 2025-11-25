
// Samme regex som er brukt i tjenesten som vi sender notifikasjoner til
val epostRegex = Regex("""^[\p{L}\p{N}._%+-]+@[\p{L}\p{N}.-]+\.\p{L}{2,}$""", RegexOption.IGNORE_CASE)

fun erGyldigEpostadresse(epost: String): Boolean = epostRegex.matches(epost.trim())
