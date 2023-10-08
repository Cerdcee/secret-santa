import logic.*
import org.kosat.Kosat

class SortingSatSolverService<P : LogicalVariable, T : Any> {

    val variables = emptyMap<Int, P>().toMutableMap()

    fun sort(
        items: List<T>,
        computeVariables: (List<T>) -> List<P>,
        computeConstraints: (List<T>) -> LogicalExpression
    ): List<P> {
        val solver = Kosat(mutableListOf(), 0)

        // Allocate variables
        solver.addVariables(items, computeVariables)

        // Add constraints
        solver.addConstraints(items, computeConstraints)

        // Solve the SAT problem for the first time
        val models: MutableList<List<P>> = mutableListOf()
        var satisfiable = solver.solve()

        // Get the model:
        // TODO : Set timeout
        while (satisfiable) {
            val model = solver.getModel()
            models += listOf(
                // Keep only true assertions
                // If a variable is negative, then is not displayed because not found in variables
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
        return models.first()
    }

    /*************************/

    private fun Kosat.addVariables(items: List<T>, computeVariables: (List<T>) -> List<P>) {
        computeVariables(items)
            .forEach { variableValue ->
                addVariable()
                    .let { variableIndex -> variables[variableIndex] = variableValue }
            }
    }

    private fun Kosat.addConstraints(items: List<T>, computeConstraints: (List<T>) -> LogicalExpression) {
        computeConstraints(items)
            .toCNF()
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