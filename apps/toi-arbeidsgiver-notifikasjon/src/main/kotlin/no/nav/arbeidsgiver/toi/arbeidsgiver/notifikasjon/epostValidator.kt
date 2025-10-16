

// En mindre streng epost validering: kun krav om én @ og minst ett punktum i domene-delen.
val epostRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun erGyldigEpostadresse(epost: String): Boolean = epostRegex.matches(epost.trim())
