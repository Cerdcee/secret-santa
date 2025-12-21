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

    val participants = readResourceFileAsString(filename)
        .let { mapper.readValue<List<Person>>(it) }
//        .let { people -> sortingService.assignPeople(people, nbGiftsPerPerson) }
//        .groupBy({ it.person }, { it.linkedPerson })
//        .also { File(backupFilename).writeText(it.toHumanReadable()) } // Write to backup file
//        .onEach { emailService.sendEmail(it.key, it.value) }


    // TODO use json to backup
    readBackupFile(readResourceFileAsString("secret_santa.backup"), participants)
}

fun readBackupFile(backupFile: String, participants: List<Person>) = //: Map<Person, List<Person>> =
    backupFile.split("\n")
        .map { personLine ->
            val gifter = personLine.split(" -> ")[0]
            val giftees = personLine.split(" -> ")[1].split(", ")

            println("gifter : $gifter")
            println("giftees : $giftees")

            findById(gifter, participants) to giftees.map { findById(it, participants) }
        }
        .let { println(it) }
//.groupBy({it.first}, {it.second})

fun findById(personId: String, people: List<Person>): Person =
    people.find { it.id == personId }
        ?: throw IllegalArgumentException("Could not find $personId in $people")