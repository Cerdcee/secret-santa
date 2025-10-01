import logic.*
import org.kosat.Kosat
import utils.measureTimeMillis

class SortingSatSolverService<P : LogicalVariable, T : Any> {

    private val MAX_COMPUTATION_TIME_MS: Long = 1000 * 60 * 20 // 20 min
    val variables = emptyMap<Int, LogicalVariable>().toMutableMap()

    fun sort(
        items: List<T>,
        nbAssociations: Int,
        computeVariables: (List<T>) -> List<P>,
        computeConstraints: (newRandomVariable: () -> RandomVariable, List<T>, Int) -> LogicalExpression
    ): List<LogicalVariable> {
        val solver = Kosat(mutableListOf(), 0)

        // Allocate variables
        measureTimeMillis({ time -> println(">>> addVariables : $time ms") }) {
            solver.addVariables(items, computeVariables)
        }

        // Add constraints
        measureTimeMillis({ time -> println(">>> addConstraints : $time ms") }) {
            solver.addConstraints(items, nbAssociations, computeConstraints)
        }

        // Solve the SAT problem for the first time
        var nbTimesSolved: Int = 0
        var computationTimeMs: Long = 0
        val models: MutableList<List<LogicalVariable>> = mutableListOf()
        var satisfiable = measureTimeMillis({ time ->
            nbTimesSolved++
            computationTimeMs += time
            println(">>> Round $nbTimesSolved solving time : $computationTimeMs ms")
        }) {
            solver.solve()
        }

        // Get the model:
        while (satisfiable && computationTimeMs < MAX_COMPUTATION_TIME_MS) {
            val model = solver.getModel()
            models += listOf(
                // Keep only true assertions
                // If a variable is negative, then is not displayed because not found in variables
                model.mapNotNull { variable -> variables[variable] }
            )

            // To get all possible solutions, run again with one more constraint : NOT(found model)
            // But remove constraints related to unnamed variables introduced for speed reasons
            // (simplification of OR toCNF())
            val foundModelNegation = model
                .filterNot { variables[it] is RandomVariable || variables[it * (-1)] is RandomVariable }
                .map { it * (-1) }
            solver.addClause(foundModelNegation)
            //  Run until result is false (unsatisfiable)
            satisfiable = measureTimeMillis({ time ->
                nbTimesSolved++
                computationTimeMs += time
                println(">>> Round $nbTimesSolved solving time : $computationTimeMs ms")
            }) {
                solver.solve()
            }
        }

        println(">>> Total solving time : $computationTimeMs ms")

        // Pick one model at random among all found models
        // TODO algorithm to find a model with the most diverse results
        models.shuffle()
        return models.firstOrNull()
            ?: throw UnsatisfiableConstraintsException()
    }

    /*************************/

    private fun Kosat.addVariables(items: List<T>, computeVariables: (List<T>) -> List<P>) {
        computeVariables(items)
            .forEach { variableValue ->
                addVariable()
                    .let { variableIndex -> variables[variableIndex] = variableValue }
            }
    }

    private fun Kosat.addConstraints(
        items: List<T>,
        nbGiftsPerPerson: Int,
        computeConstraints: (newRandomVariable: () -> RandomVariable, List<T>, Int) -> LogicalExpression
    ) {
        computeConstraints(
            {
                // TODO add auxiliary function for this
                addVariable().let { variableIndex ->
                    RandomVariable(variableIndex).also { variables[variableIndex] = it }
                }
            },
            items,
            nbGiftsPerPerson
        )
            .toCNF() // TODO use toFastCNF() and remove all toCNF()
            .let { toDimacs(it) }
            .forEach { addClause(it) }
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
            is LogicalVariable -> matchVariable()
            is NOT -> if (a is LogicalVariable) {
                a.matchVariable() * (-1)
            } else {
                throw IllegalStateException("Cannot convert NOT to DIMACS format if was not simplified first")
            }

            else -> throw IllegalStateException("Cannot convert non-primitive LogicalExpression to DIMACS format : $this")
        }

    private fun LogicalVariable.matchVariable(): Int =
        variables.filter { (_, value) -> value == this }
            .firstNotNullOf { (key, _) -> key }
}

private fun Set<Int>.hasTruthyStatement(): Boolean = any { contains(it * (-1)) }

fun List<LogicalExpression>.joinToLogicalExpression(logicalExpressionConstructor: (LogicalExpression, LogicalExpression) -> LogicalExpression): LogicalExpression =
    when (size) {
        0 -> throw IllegalArgumentException("joinToLogicalExpression() cannot operate on an empty list")
        1 -> first()
        else -> {
            fold<LogicalExpression, LogicalExpression?>(
                initial = null,
                operation = { acc, logicalExpression ->
                    if (acc == null) {
                        logicalExpression
                    } else {
                        logicalExpressionConstructor(acc, logicalExpression)
                    }
                }
            )
                ?: throw IllegalStateException("joinToLogicalExpression() cannot return null")
        }
    }