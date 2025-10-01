import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.Person
import email.EmailService
import utils.readResourceFileAsString
import utils.toHumanReadable
import java.io.File

fun main() {
    // VARIABLES TO CHANGE //
    val filename = "example.json"
    val backupFilename = "secret_santa.backup"
    val nbGiftsPerPerson = 3
    // ******************* //

    val mapper = jacksonObjectMapper()
    val sortingService = PeopleSortingService()
    val emailService = EmailService()

    readResourceFileAsString(filename)
        .let { mapper.readValue<List<Person>>(it) }
        .let { people -> sortingService.assignPeople(people, nbGiftsPerPerson) }
        .groupBy({ it.person }, { it.linkedPerson })
        .also { File(backupFilename).writeText(it.toHumanReadable()) } // Write to backup file
        .onEach { emailService.sendEmail(it.key, it.value) }
}

