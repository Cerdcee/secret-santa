import data.Person
import logic.*
import org.kosat.Kosat

class SortingSatSolverService {

    fun assignPeople(people: List<Person>, nbGiftsPerPerson: Int): List<List<Pairing>> {
        val solver = Kosat(mutableListOf(), 0)

        // Allocate variables
        solver.addVariables(people)

        // Add constraints
        // 1) Everyone receives a gift
        solver.addConstraints(people, 1)
        // 2) Everyone receives multiple gifts (no duplicates)
        // 3) Add personal constraints (force or forbid gifts between people)

        // Solve the SAT problem:
        val result = solver.solve()
        println("Result = $result")

        // Get the model:
        if (result) {
            val model = solver.getModel()
            println("Model = $model")

            // TODO: To get all possible solutions, run again with one more constraint : NOT(found model)
            //  Run until result is false (unsolvable)
            //  Then pick one model a random

            return listOf(
                // Display only true assertions
                // If variable is negative, then is not displayed because not found in variables
                model.mapNotNull { variable -> variables[variable] }
            )
        }

        return emptyList()
    }

    /*************************/

    // A gifts to B is a variable --> Xab
    // Order is important : A gifts to B =! B gifts to A
    // There should be a variable for each possibility of one person giving a gift to another
    private fun Kosat.addVariables(people: List<Person>) {
        people.forEach { personGiving ->
            people.forEach { personReceivingGift ->
                if (personGiving.id != personReceivingGift.id) {
                    addVariable()
                        .let { variable -> variables[variable] = Pairing(personGiving, personReceivingGift) }
                }
            }
        }

        variables.print()
    }

    // Add (A gifts to B) XOR (A gifts to C) XOR ...
    // But translated into CNF with only AND, OR, NOT
    private fun Kosat.addConstraints(people: List<Person>, nbGiftsPerPerson: Int) {
        val constraints: MutableList<LogicalExpression> = mutableListOf()

        people.forEach { person ->
            println(person.firstName)
            constraints += computeConstraints(person, people, nbGiftsPerPerson)
        }

        constraints
            .joinToLogicalExpression { a, b -> AND(a, b) }
            .let { toDimacs(it) }
            .also { println(it) }
            .forEach { addClause(it) }
    }

    private fun computeConstraints(person: Person, people: List<Person>, nbGiftsPerPerson: Int): LogicalExpression {
        val personPairings = variables.values.toList().filter { it.person.id == person.id }

        val personGivesToAtLeastOnePerson = personPairings.joinToLogicalExpression { a, b -> OR(a, b) }

        val personDoesNotGiveToSpecificPersonList =
            people.filter { it.id != person.id }
                .map { otherPerson ->
                    personPairings.filter { it.linkedPerson.id != otherPerson.id }
                        .joinToLogicalExpression { a, b -> OR(NOT(a), NOT(b)) }
                }
                .joinToLogicalExpression { a, b -> AND(a, b) }

        val personGivesExactlyOneGift =
            AND(personGivesToAtLeastOnePerson, personDoesNotGiveToSpecificPersonList)

        // ---

        val linkedPersonPairings = variables.values.toList().filter { it.linkedPerson.id == person.id }

        val personReceivesFromAtLeastOnePerson = linkedPersonPairings.joinToLogicalExpression { a, b -> OR(a, b) }

        val personDoesNotReceiveFromSpecificPersonList =
            people.filter { it.id != person.id }
                .map { otherPerson ->
                    linkedPersonPairings.filter { it.person.id != otherPerson.id }
                        .joinToLogicalExpression { a, b -> OR(NOT(a), NOT(b)) }
                }
                .joinToLogicalExpression { a, b -> AND(a, b) }

        val personReceivesExactlyOneGift =
            AND(personReceivesFromAtLeastOnePerson, personDoesNotReceiveFromSpecificPersonList)

        return AND(personGivesExactlyOneGift, personReceivesExactlyOneGift)
    }

    fun toDimacs(expr: LogicalExpression): List<Set<Int>> =
        toORArray(expr).map { exprOR ->
            toPrimitiveArray(exprOR)
                .mapNotNull { it.toDimacs() }
                .toSet()
        }
            // Simplify CNF
            .filter { !it.hasTruthyStatement() }
            .filter { it.isNotEmpty() }

    private fun LogicalExpression.toDimacs(): Int? =
        when (this) {
            is TRUE -> null
            is Pairing -> matchVariable()
            is NOT -> if (a is Pairing) {
                a.matchVariable() * (-1)
            } else {
                throw IllegalStateException("Cannot convert NOT to DIMACS format if was not simplified first")
            }

            else -> throw IllegalStateException("Cannot convert non-primitive LogicalExpression (other than [Pairing, NOT] to DIMACS format : $this")
        }

    private fun Pairing.matchVariable(): Int =
        variables.filter { (key, value) -> value == this }
            .firstNotNullOf { (key, value) -> key }

    companion object {
        val variables = emptyMap<Int, Pairing>().toMutableMap()
    }
}

private fun Set<Int>.hasTruthyStatement(): Boolean = any { contains(it * (-1)) }

private fun Map<Int, Pairing>.print() {
    println("\nVariables :")
    map { (key, pairing) -> "[$key] ${pairing.person.id} -> ${pairing.linkedPerson.id}" }
        .joinToString(separator = "\n")
        .let { println(it) }
    println("-----")
}

fun List<LogicalExpression>.joinToLogicalExpression(logicalExpressionConstructor: (LogicalExpression, LogicalExpression) -> LogicalExpression): LogicalExpression =
    this.fold<LogicalExpression, LogicalExpression?>(
        initial = null,
        operation = { acc, logicalExpression ->
            if (acc == null) {
                logicalExpression
            } else {
                logicalExpressionConstructor(acc, logicalExpression)
            }
        }
    )
        ?: throw IllegalStateException("joinToLogicalExpression cannot return null")