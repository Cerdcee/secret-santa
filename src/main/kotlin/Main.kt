import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.Person
import java.io.FileNotFoundException

fun main() {
    val mapper = jacksonObjectMapper()

    readResourceFile("example.json")
        .let { mapper.readValue<List<Person>>(it) }
        .onEach { println(it) } // List<Person>

}

private fun readResourceFile(filename: String): String =
    object {}.javaClass.classLoader.getResource(filename)
        ?.readText()
        ?: throw FileNotFoundException("File $filename was not found in /main/resources")