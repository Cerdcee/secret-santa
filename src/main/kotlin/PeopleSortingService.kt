import data.Person
import data.RequestType.GIFT_TO
import data.RequestType.NO_GIFT_TO
import logic.*

class PeopleSortingService {

    // Constraints :
    // 1) Everyone receives "nbGiftsPerPerson" gifts
    // 2) One person does not receive more than one gift from a specific person
    // 3) Add personal constraints (force or forbid gifts between people)
    // TODO multiple gifts
    fun assignPeople(people: List<Person>, nbGiftsPerPerson: Int): List<Pairing> {
        val satSolverService = SortingSatSolverService<Pairing, Person>()
        return satSolverService.sort(people, ::computeVariables, this::computeConstraints)
    }

    /*************************/

    // A gifts to B is a variable --> Xab
    // Order is important : A gifts to B =! B gifts to A
    // There should be a variable for each possibility of one person giving a gift to another
    private fun computeVariables(people: List<Person>): List<Pairing> {
        val variables: MutableList<Pairing> = mutableListOf()

        people.forEach { personGiving ->
            people.forEach { personReceivingGift ->
                if (personGiving.id != personReceivingGift.id) {
                    variables += Pairing(personGiving, personReceivingGift)
                }
            }
        }

        // Store for later use in computeConstraints
        pairings = variables

        return variables
    }

    // Add (A gifts to B) XOR (A gifts to C) XOR ...
    // But translated into CNF with only AND, OR, NOT
    private fun computeConstraints(people: List<Person>): LogicalExpression {
        val constraints: MutableList<LogicalExpression> = mutableListOf()

        people.forEach { person ->
            constraints += computeConstraints(person, people)
        }

        return constraints.joinToLogicalExpression { a, b -> AND(a, b) }
    }

    //  Use DNF :
    //  Alice gives to exactly one person :
    //  B: Alice gives to Bob / C: Alice gives to Charles / D: Alice gives to Dolores
    //  (B AND NOT(C) AND NOT(D)) OR (NOT(B) AND C AND NOT(D)) OR (NOT(B) AND NOT(C) AND D)
    //  Then convert to CNF and join with ANDs
    //  Check if works with 3 people
    private fun computeConstraints(person: Person, people: List<Person>): LogicalExpression {
        val personConstraints = mutableListOf<LogicalExpression>()

        // Set personal constraints
        // NO_GIFT_TO : cannot give to specific person
        person.requests
            .filter { it.type == NO_GIFT_TO }
            .forEach { request -> personConstraints += NOT(Pairing(person, people.findPerson(request.otherPersonId))) }

        // Make sure person gives to only one (other) person
        // Also check for personal constraints : GIFT_TO
        person.requests
            .filter { it.type == GIFT_TO }
            .let { giftToRequests ->
                personConstraints += when (giftToRequests.size) {
                    0 -> person.givesToExactlyOneAmong(people)
                    1 -> person.givesToExactlyOneAmong(listOf(people.findPerson(giftToRequests.first().otherPersonId)))
                    else -> throw IllegalStateException("${person.id} cannot give to more than 1 person, is requested to give to ${giftToRequests.map { it.otherPersonId }}")
                }
            }

        // Make sure person receives from one (other) person
        personConstraints += person.receivesFromExactlyOneAmong(people)

        return personConstraints.joinToLogicalExpression { a, b -> AND(a, b) }
    }

    private fun Person.givesToExactlyOneAmong(people: List<Person>): LogicalExpression {
        val personPairings: List<Pairing> = pairings.filter { it.person.id == id }

        return people.filter { it.id != id } // Keep all other people except "person"
            .map { otherPerson ->
                // For each other person Xn, add the constraint
                // NOT(X1) AND ... AND NOT(Xn-1) AND Xn AND NOT(Xn+1) AND ... AND NOT(Xm)
                personPairings.joinToLogicalExpression { a, b ->
                    val newA = if (a !is Pairing || a.linkedPerson.id == otherPerson.id) a else NOT(a)
                    val newB = if (b !is Pairing || b.linkedPerson.id == otherPerson.id) b else NOT(b)
                    AND(newA, newB)
                }
            }
            .joinToLogicalExpression { a, b -> OR(a, b) }
            .toCNF()
    }

    private fun Person.receivesFromExactlyOneAmong(people: List<Person>): LogicalExpression {
        val linkedPersonPairings = pairings.filter { it.linkedPerson.id == id }

        return people.filter { it.id != id } // Keep all other people except "person"
            .map { otherPerson ->
                // For each other person Xn, add the constraint
                // NOT(X1) AND ... AND NOT(Xn-1) AND Xn AND NOT(Xn+1) AND ... AND NOT(Xm)
                linkedPersonPairings.joinToLogicalExpression { a, b ->
                    val newA = if (a !is Pairing || a.person.id == otherPerson.id) a else NOT(a)
                    val newB = if (b !is Pairing || b.person.id == otherPerson.id) b else NOT(b)
                    AND(newA, newB)
                }
            }
            .joinToLogicalExpression { a, b -> OR(a, b) }
            .toCNF()
    }

    companion object {
        var pairings: List<Pairing> = emptyList()
    }
}

private fun List<Person>.findPerson(personId: String): Person {
    val peopleWithPersonId = filter { it.id == personId }

    return when (peopleWithPersonId.size) {
        0 -> throw IllegalArgumentException("No person found with id $personId")
        1 -> peopleWithPersonId.first()
        else -> throw IllegalArgumentException("Found ${peopleWithPersonId.size} people with id $personId")
    }
}