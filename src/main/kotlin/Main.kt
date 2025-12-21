import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import data.Person
import email.EmailService
import sorting.SolverService
import sorting.satSolver.PeopleSortingSatSolverService
import utils.readResourceFileAsString
import utils.toHumanReadable
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

fun main() {
    val clock = Clock.system(ZoneId.of("Europe/Paris"))

    // VARIABLES TO CHANGE //
    val filename = "example.json"
    val backupFilename = "secret_santa_${Instant.now(clock)}.backup"
    val nbGiftsPerPerson = 3
    // ******************* //

    val mapper = jacksonObjectMapper()
    // val sortingService: SortingService = RandomSolverSortingService()
    val sortingService: SolverService = PeopleSortingSatSolverService()
    val emailService = EmailService()

    readResourceFileAsString(filename)
        .let { mapper.readValue<List<Person>>(it) }
        .let { people -> sortingService.assignPeople(people, nbGiftsPerPerson) }
        .groupBy({ it.person }, { it.linkedPerson })
        .also { File(backupFilename).writeText(it.toHumanReadable()) } // Write to backup file
        .onEach { emailService.sendEmail(it.key, it.value) }
}

