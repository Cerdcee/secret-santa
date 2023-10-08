import data.Person
import logic.*
import org.kosat.Kosat

class SortingSatSolverService {

    fun assignPeople(people: List<Person>, nbGiftsPerPerson: Int): List<Pairing> {
        val solver = Kosat(mutableListOf(), 0)

        // Allocate variables
        solver.addVariables(people)

        // Add constraints
        // 1) Everyone receives a gift
        solver.addConstraints(people, 1)
        // 2) Everyone receives multiple gifts (no duplicates)
        // 3) Add personal constraints (force or forbid gifts between people)

        // Solve the SAT problem:
        val models: MutableList<List<Pairing>> = mutableListOf()
        var satisfiable = solver.solve()

        // Get the model:
        // TODO : Set timeout
        while (satisfiable) {
            val model = solver.getModel()
            models += listOf(
                // Keep only true assertions
                // If variable is negative, then is not displayed because not found in variables
                model.mapNotNull { variable -> variables[variable] }
            )

            // To get all possible solutions, run again with one more constraint : NOT(found model)
            val foundModelNegation = model.map { it * (-1) }
            solver.addClause(foundModelNegation)
            //  Run until result is false (unsatisfiable)
            satisfiable = solver.solve()
        }

        // Pick one model at random among all found models
        models.shuffle()
        return models.first() // Return one model at random
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
    }

    // Add (A gifts to B) XOR (A gifts to C) XOR ...
    // But translated into CNF with only AND, OR, NOT
    private fun Kosat.addConstraints(people: List<Person>, nbGiftsPerPerson: Int) {
        val constraints: MutableList<LogicalExpression> = mutableListOf()

        people.forEach { person ->
            constraints += computeConstraints(person, people, nbGiftsPerPerson)
        }

        constraints
            .joinToLogicalExpression { a, b -> AND(a, b) }
            .let { toDimacs(it) }
            .forEach { addClause(it) }
    }

    //  Use DNF :
    //  Alice gives to exactly one person :
    //  B: Alice gives to Bob / C: Alice gives to Charles / D: Alice gives to Dolores
    //  (B AND NOT(C) AND NOT(D)) OR (NOT(B) AND C AND NOT(D)) OR (NOT(B) AND NOT(C) AND D)
    //  Then convert to CNF and join with ANDs
    //  Check if works with 3 people
    private fun computeConstraints(person: Person, people: List<Person>, nbGiftsPerPerson: Int): LogicalExpression {
        val personPairings = variables.values.toList().filter { it.person.id == person.id }

        val personGivesToExactlyOnePerson =
            people.filter { it.id != person.id } // Keep all other people except "person"
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

        // ---

        val linkedPersonPairings = variables.values.toList().filter { it.linkedPerson.id == person.id }

        val personReceivesFromExactlyOnePerson =
            people.filter { it.id != person.id } // Keep all other people except "person"
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

        return AND(personGivesToExactlyOnePerson, personReceivesFromExactlyOnePerson)
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
        variables.filter { (_, value) -> value == this }
            .firstNotNullOf { (key, _) -> key }

    companion object {
        val variables = emptyMap<Int, Pairing>().toMutableMap()
    }
}

private fun Set<Int>.hasTruthyStatement(): Boolean = any { contains(it * (-1)) }

fun List<LogicalExpression>.joinToLogicalExpression(logicalExpressionConstructor: (LogicalExpression, LogicalExpression) -> LogicalExpression): LogicalExpression =
    if (size <= 1) {
        throw IllegalArgumentException("joinToLogicalExpression() must operate on a list of at least 2 elements")
    } else {
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
    }