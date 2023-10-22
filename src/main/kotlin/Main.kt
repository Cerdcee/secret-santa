import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.Person
import email.EmailService
import utils.readResourceFileAsString
import utils.toHumanReadable

fun main() {
    val mapper = jacksonObjectMapper()
    val sortingService = PeopleSortingService()
    val emailService = EmailService()

    readResourceFileAsString("people_test.json")
        .let { mapper.readValue<List<Person>>(it) }
        .let { people -> sortingService.assignPeople(people, 1) }
        .also { println(it.toHumanReadable()) }
        .groupBy({ it.person }, { it.linkedPerson })
        .onEach { emailService.sendEmail(it.key, it.value) }

    // TODO Write to backup file + SH script to grep
}
