package sorting

import data.Person
import logic.Pairing

abstract class SolverService {

    // Constraints :
    // 1) Everyone receives "nbGiftsPerPerson" gifts
    // 2) One person does not receive more than one gift from a specific person
    // 3) People have personal constraints (force or forbid gifts to someone)
    // TODO add timeout duration in params
    abstract fun assignPeople(people: List<Person>, nbGiftsPerPerson: Int): List<Pairing>

    protected fun List<Person>.findPerson(personId: String): Person {
        val peopleWithPersonId = filter { it.id == personId }

        return when (peopleWithPersonId.size) {
            0 -> throw IllegalArgumentException("No person found with id $personId")
            1 -> peopleWithPersonId.first()
            else -> throw IllegalArgumentException("Found ${peopleWithPersonId.size} people with id $personId")
        }
    }
}