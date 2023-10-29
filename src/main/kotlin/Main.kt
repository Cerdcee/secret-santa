import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.Person
import email.EmailService
import utils.readResourceFileAsString
import utils.toHumanReadable
import java.io.File

fun main() {
    val mapper = jacksonObjectMapper()
    val sortingService = PeopleSortingService()
    val emailService = EmailService()

    readResourceFileAsString("example.json")
        .let { mapper.readValue<List<Person>>(it) }
        .let { people -> sortingService.assignPeople(people, 1) }
        .also { println(it.toHumanReadable()) }
        .groupBy({ it.person }, { it.linkedPerson })
        .also {
            // Write to backup file
            File("secret_santa.backup").writeText(it.toHumanReadable())
        }
        .onEach { emailService.sendEmail(it.key, it.value) }
}
