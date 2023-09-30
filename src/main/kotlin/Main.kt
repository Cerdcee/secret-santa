import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.Person
import java.io.FileNotFoundException

fun main() {
    val mapper = jacksonObjectMapper()
    val sortingService = SortingSatSolverService()

    readResourceFile("people.json")
        .let { mapper.readValue<List<Person>>(it) }
        .onEach { println(it) } // List<Person>
        .let { people -> sortingService.assignPeople(people, 3) }
        .onEach { println(it) } // Map<Person, List<Person>>
    // .onEach { pairing -> sendEmails(pairing) }
}

private fun readResourceFile(filename: String): String =
    object {}.javaClass.classLoader.getResource(filename)
        ?.readText()
        ?: throw FileNotFoundException("File $filename was not found in /main/resources")