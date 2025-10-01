package logic

import sorting.SortingSatSolverService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import kotlin.test.assertIsNot

class LogicalExpressionTest {

    val sortingService = SortingSatSolverService<DummyExpression, String>()

    @Nested
    inner class ConvertToDimacs {

        @BeforeEach
        fun clearVariables(){
            sortingService.variables.clear()
            sortingService.variables +=
                mapOf(
                    1 to DummyExpression("alice", "bob"),
                    2 to DummyExpression("alice", "charles"),
                    3 to DummyExpression("bob", "alice"),
                    4 to DummyExpression("bob", "charles"),
                    5 to DummyExpression("charles", "alice"),
                    6 to DummyExpression("charles", "bob"),
                )
        }

        @Test
        fun `convert logical expression to DIMACS format`() {
            val expr =
                AND(
                    AND(
                        AND(
                            OR(DummyExpression("alice", "bob"), DummyExpression("alice", "charles")),
                            OR(NOT(DummyExpression("alice", "charles")), NOT(DummyExpression("alice", "bob")))
                        ),
                        AND(
                            OR(DummyExpression("bob", "alice"), DummyExpression("bob", "charles")),
                            OR(NOT(DummyExpression("bob", "charles")), NOT(DummyExpression("bob", "alice")))
                        )
                    ),
                    AND(
                        OR(DummyExpression("charles", "alice"), DummyExpression("charles", "bob")),
                        OR(NOT(DummyExpression("charles", "bob")), NOT(DummyExpression("charles", "alice")))
                    )
                )

            val dimacsExpr = sortingService.toDimacs(expr)

            expectThat(dimacsExpr).containsExactlyInAnyOrder(
                listOf(
                    setOf(1, 2), setOf(-2, -1), setOf(3, 4), setOf(-4, -3), setOf(5, 6), setOf(-6, -5)
                )
            )
        }
    }

    @Nested
    inner class ConvertToCNF {

        @BeforeEach
        fun clearVariables(){
            sortingService.variables.clear()
            sortingService.variables +=
                mapOf(
                    1 to DummyExpression("alice", "bob"),
                    2 to DummyExpression("alice", "charles"),
                    3 to DummyExpression("alice", "diana"),
                )
        }

        @Test
        fun `convert XOR to CNF`() {
            val expr =
                XOR(
                    XOR(
                        DummyExpression("alice", "bob"),
                        DummyExpression("alice", "charles")
                    ),
                    DummyExpression("alice", "diana")
                )

            val exprCNF = expr.toCNF()
            val exprDimacs = sortingService.toDimacs(exprCNF)

            expectThat(exprDimacs).containsExactlyInAnyOrder(
                listOf(
                    setOf(-1, -2, 3), setOf(-1, 2, -3), setOf(1, -2, -3), setOf(1, 2, 3)
                )
            )
        }

        @Test
        fun `convert logical expression to CNF`() {
            val expr =
                AND(
                    AND(
                        XOR(
                            DummyExpression("alice", "bob"),
                            DummyExpression("alice", "charles")
                        ),
                        XOR(
                            DummyExpression("bob", "alice"),
                            DummyExpression("bob", "charles")
                        )
                    ),
                    XOR(
                        DummyExpression("charles", "alice"),
                        DummyExpression("charles", "bob")
                    )
                )

            val exprCNF = expr.toCNF()

            checkNOTOnlyAtPrimitiveLevel(exprCNF)
            checkParentExpressionIsAND(exprCNF)
            checkNoANDInsideORs(exprCNF)
        }
    }
}

private fun checkNOTOnlyAtPrimitiveLevel(expr: LogicalExpression) {
    when (expr) {
        is NOT -> expectThat(expr.a).isA<DummyExpression>()
        is DummyExpression -> {}
        is OR -> {
            checkNOTOnlyAtPrimitiveLevel(expr.a)
            checkNOTOnlyAtPrimitiveLevel(expr.b)
        }

        is AND -> {
            checkNOTOnlyAtPrimitiveLevel(expr.a)
            checkNOTOnlyAtPrimitiveLevel(expr.b)
        }

        else -> throw IllegalArgumentException("Illegal LogicalExpression in a CNF : $expr")
    }
}

private fun checkParentExpressionIsAND(expr: LogicalExpression) {
    expectThat(expr).isA<AND>()
}

private fun checkNoANDInsideORs(expr: LogicalExpression) {
    when (expr) {
        is NOT, is DummyExpression -> {}
        is OR -> {
            assertIsNot<AND>(expr.a)
            assertIsNot<AND>(expr.b)
            checkNoANDInsideORs(expr.a)
            checkNoANDInsideORs(expr.b)
        }

        is AND -> {
            checkNoANDInsideORs(expr.a)
            checkNoANDInsideORs(expr.b)
        }

        else -> throw IllegalArgumentException("Illegal LogicalExpression in a CNF : $expr")
    }
}
