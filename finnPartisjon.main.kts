


fun finnPartisjon(aktørId: String, antallPartisjoner: Int = 6): Int {
    val hash = murmur2(aktørId.toByteArray(Charsets.UTF_8))
    return (hash and 0x7fffffff) % antallPartisjoner
}

fun murmur2(data: ByteArray): Int {
    val length = data.size
    val seed = 0x9747b28c.toInt()
    val m = 0x5bd1e995
    val r = 24

    var h = seed xor length
    val length4 = length / 4

    for (i in 0 until length4) {
        val i4 = i * 4
        var k = (data[i4].toInt() and 0xff) or
            ((data[i4 + 1].toInt() and 0xff) shl 8) or
            ((data[i4 + 2].toInt() and 0xff) shl 16) or
            ((data[i4 + 3].toInt() and 0xff) shl 24)
        k *= m
        k = k xor (k ushr r)
        k *= m
        h *= m
        h = h xor k
    }

    val offset = length4 * 4
    when (length % 4) {
        3 -> {
            h = h xor ((data[offset + 2].toInt() and 0xff) shl 16)
            h = h xor ((data[offset + 1].toInt() and 0xff) shl 8)
            h = h xor (data[offset].toInt() and 0xff)
            h *= m
        }
        2 -> {
            h = h xor ((data[offset + 1].toInt() and 0xff) shl 8)
            h = h xor (data[offset].toInt() and 0xff)
            h *= m
        }
        1 -> {
            h = h xor (data[offset].toInt() and 0xff)
            h *= m
        }
    }

    h = h xor (h ushr 13)
    h *= m
    h = h xor (h ushr 15)

    return h
}

if (args.isEmpty()) {
    println("Bruk: oppgi en eller flere aktørId-er som programargumenter, eller kall finnPartisjon(\"<aktørId>\") direkte.")
} else {
    for (aktørId in args) {
        println("$aktørId -> partisjon ${finnPartisjon(aktørId)} (av 6 i prod)")
    }
}
