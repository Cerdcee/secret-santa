import data.Person
import data.RequestType.GIFT_TO
import data.RequestType.NO_GIFT_TO
import logic.*
import utils.measureTimeMillis

class PeopleSortingService {

    // Constraints :
    // 1) Everyone receives "nbGiftsPerPerson" gifts
    // 2) One person does not receive more than one gift from a specific person
    // 3) Add personal constraints (force or forbid gifts between people)
    fun assignPeople(people: List<Person>, nbGiftsPerPerson: Int): List<Pairing> {
        val satSolverService = SortingSatSolverService<Pairing, Person>()
        return satSolverService.sort(people, nbGiftsPerPerson, ::computeVariables, this::computeConstraints)
            .filterIsInstance<Pairing>()
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
    private fun computeConstraints(newRandomVariable: () -> RandomVariable, people: List<Person>, nbGiftsPerPerson: Int): LogicalExpression {
        val constraints: MutableList<LogicalExpression> = mutableListOf()

        people.forEach { person ->
            val personConstraints = measureTimeMillis({ time -> println(">>> computeConstraint for ${person.firstName} : $time ms") }) {
                println("Start computing constraint for ${person.firstName}")
                computeConstraints(newRandomVariable, person, people, nbGiftsPerPerson)
            }
            constraints += personConstraints
        }

        return constraints.joinToLogicalExpression { a, b -> AND(a, b) }
    }

    //  Use DNF :
    //  Alice gives to exactly one person :
    //  B: Alice gives to Bob / C: Alice gives to Charles / D: Alice gives to Dolores
    //  (B AND NOT(C) AND NOT(D)) OR (NOT(B) AND C AND NOT(D)) OR (NOT(B) AND NOT(C) AND D)
    //  Then convert to CNF and join with ANDs
    private fun computeConstraints(newRandomVariable: () -> RandomVariable, person: Person, people: List<Person>, nbGiftsPerPerson: Int): LogicalExpression {
        val personConstraints = mutableListOf<LogicalExpression>()

        // Set personal constraints
        // NO_GIFT_TO : cannot give to specific person
        person.requests
            .filter { it.type == NO_GIFT_TO }
            .forEach { request -> personConstraints += NOT(Pairing(person, people.findPerson(request.otherPersonId))) }

        // Make sure person gives to only nbGiftsPerPerson (other) person
        // Also check for personal constraints : GIFT_TO
        person.requests
            .filter { it.type == GIFT_TO }
            .let { giftToRequests ->
                personConstraints += if (giftToRequests.isEmpty()) {
                    person.isPairedToExactlyXAmong(newRandomVariable, pairings, nbGiftsPerPerson, isGiving = true)
                } else {
                    if (giftToRequests.size > nbGiftsPerPerson) {
                        throw IllegalStateException(
                            "${person.id} cannot give to more than $nbGiftsPerPerson " +
                                    "${if (nbGiftsPerPerson <= 1) "person" else "people"}, is requested to give to " +
                                    "${giftToRequests.map { it.otherPersonId }}"
                        )
                    } else {
                        // Restrict pairings only to people present in giftToRequests
                        val giftToPairings = pairings.filter { pairing ->
                            pairing.linkedPerson.id in giftToRequests.map { it.otherPersonId }
                        }
                        person.isPairedToExactlyXAmong(newRandomVariable, giftToPairings, nbGiftsPerPerson, isGiving = true)
                    }
                }
            }

        // Make sure person receives from nbGiftsPerPerson (other) people
        personConstraints += person.isPairedToExactlyXAmong(newRandomVariable, pairings, nbGiftsPerPerson, isGiving = false)

        return personConstraints.joinToLogicalExpression { a, b -> AND(a, b) }
    }

    companion object {
        var pairings: List<Pairing> = emptyList()
    }
}

// TODO remove intermediary variables
private fun Person.isPairedToExactlyXAmong(
    newRandomVariable: () -> RandomVariable,
    pairings: List<Pairing>,
    nbGiftsPerPerson: Int,
    isGiving: Boolean
): LogicalExpression {
    // Keep pairings with person as the first person in the pairings if is the one giving
    // Else keep pairings with person as the linked person (the one receiving)
    val filteredPairings = pairings.filter { if (isGiving) it.person.id == id else it.linkedPerson.id == id }

    // Add all the combinations of constraints where nbGiftsPerPerson pairings are true and the others are false
    val initialCombinationsList = List(filteredPairings.size) { it < nbGiftsPerPerson }
    val allCombinationsIterable = AllCombinationsIterable(initialCombinationsList)

    val constraints = mutableListOf(buildConstraintForCombination(filteredPairings, initialCombinationsList))

    while (allCombinationsIterable.hasNext()) {
        constraints.add(buildConstraintForCombination(filteredPairings, allCombinationsIterable.next()))
    }

    val orLinkedExpressions = measureTimeMillis({ time -> println(">>> joinLogicalExpressions : $time ms") }) {
        constraints.joinToLogicalExpression { a, b -> OR(a, b) }
    }

    val cnf = measureTimeMillis({ time -> println(">>> constraintsToCNF : $time ms") }) {
        orLinkedExpressions.toFastCNF(newRandomVariable)
    }

    return cnf
}

private fun buildConstraintForCombination(pairings: List<Pairing>, combination: List<Boolean>): LogicalExpression {
    val constraints = mutableListOf<LogicalExpression>()

    combination.forEachIndexed { index, keepPairing ->
        constraints += if (keepPairing) pairings[index] else NOT(pairings[index])
    }

    return constraints.joinToLogicalExpression { a, b -> AND(a, b) }
}

private fun List<Person>.findPerson(personId: String): Person {
    val peopleWithPersonId = filter { it.id == personId }

    return when (peopleWithPersonId.size) {
        0 -> throw IllegalArgumentException("No person found with id $personId")
        1 -> peopleWithPersonId.first()
        else -> throw IllegalArgumentException("Found ${peopleWithPersonId.size} people with id $personId")
    }
}