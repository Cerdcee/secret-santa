package utils

import data.Person
import logic.Pairing

fun Map<Person, List<Person>>.toHumanReadable(): String =
    map { (person, linkedPeople) ->
        person.id + " -> " + linkedPeople.joinToString(", ") { it.id }
    }
        .sortedWith { str1, str2 -> str1.compareTo(str2, ignoreCase = true) } // Sorting is useful for debug
        .joinToString("\n")

/** For debug **/
fun List<Pairing>.debugDisplay() {
    println(
        groupBy({ it.person }, { it.linkedPerson })
            .toHumanReadable()
    )
}

fun Map<Int, Pairing>.printVariables() {
    println("\nVariables :")
    map { (key, pairing) -> "[$key] ${pairing.person.id} -> ${pairing.linkedPerson.id}" }
        .joinToString(separator = "\n")
        .let { println(it) }
    println("-----")
}

inline fun <T> measureTimeMillis(
    loggingFunction: (Long) -> Unit,
    function: () -> T
): T {

    val startTime = System.currentTimeMillis()
    val result: T = function.invoke()
    loggingFunction.invoke(System.currentTimeMillis() - startTime)

    return result
}