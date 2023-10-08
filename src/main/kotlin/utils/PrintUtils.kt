package utils

import logic.Pairing

fun List<Pairing>.toHumanReadable(): String =
    joinToString(prefix = "[\n\t", separator = "\n\t", postfix = "\n]") { pairing ->
        "${pairing.person.id} -> ${pairing.linkedPerson.id}"
    }

private fun Map<Int, Pairing>.printVariables() {
    println("\nVariables :")
    map { (key, pairing) -> "[$key] ${pairing.person.id} -> ${pairing.linkedPerson.id}" }
        .joinToString(separator = "\n")
        .let { println(it) }
    println("-----")
}