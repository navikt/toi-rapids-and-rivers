package no.nav.arbeidsgiver.toi.kandidat.indekser.geografi

import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class Geografi (
    val geografikode: String,
    val navn: String
) {
    val kapitalisertNavn: String
        get() {
        val result = StringBuilder()
        val matcher: Matcher = Pattern.compile("([ (-]*)([^ (-]+)").matcher(navn.lowercase(Locale.getDefault()))
        while (matcher.find()) {
            val delimiter: String = matcher.group(1)
            val word: String = matcher.group(2)
            result.append(delimiter).append(if (Pattern.compile("og|i").matcher(word).matches()) word else capitalize(word))
        }
        return result.toString()
    }

    private fun capitalize(name: String): String {
        if (name.isEmpty()) {
            return name
        }
        val chars = name.toCharArray()
        chars[0] = chars[0].uppercaseChar()
        return String(chars)
    }
}
