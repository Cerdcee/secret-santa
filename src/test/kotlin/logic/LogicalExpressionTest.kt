package logic

import SortingSatSolverService
import alice
import bob
import charles
import diana
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import kotlin.test.assertIsNot

class LogicalExpressionTest {

    val sortingService = SortingSatSolverService()

    @Nested
    inner class ConvertToCNF {
        @Test
        fun `convert XOR to CNF`() {
            val expr =
                XOR(
                    XOR(
                        Pairing(alice, bob),
                        Pairing(alice, charles)
                    ),
                    Pairing(alice, diana)
                )
            SortingSatSolverService.variables.clear()
            SortingSatSolverService.variables +=
                mapOf(
                    1 to Pairing(alice, bob),
                    2 to Pairing(alice, charles),
                    3 to Pairing(alice, diana),
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
                            Pairing(alice, bob),
                            Pairing(alice, charles)
                        ),
                        XOR(
                            Pairing(bob, alice),
                            Pairing(bob, charles)
                        )
                    ),
                    XOR(
                        Pairing(charles, alice),
                        Pairing(charles, bob)
                    )
                )

            val exprCNF = expr.toCNF()

            checkNOTOnlyAtPrimitiveLevel(exprCNF)
            checkParentExpressionIsAND(exprCNF)
            checkNoANDInsideORs(exprCNF)
        }
    }

    @Nested
    inner class ConvertToDimacs {
        @Test
        fun `convert logical expression to DIMACS format`() {
            val expr =
                AND(
                    AND(
                        AND(
                            OR(Pairing(alice, bob), Pairing(alice, charles)),
                            OR(NOT(Pairing(alice, charles)), NOT(Pairing(alice, bob)))
                        ),
                        AND(
                            OR(Pairing(bob, alice), Pairing(bob, charles)),
                            OR(NOT(Pairing(bob, charles)), NOT(Pairing(bob, alice)))
                        )
                    ),
                    AND(
                        OR(Pairing(charles, alice), Pairing(charles, bob)),
                        OR(NOT(Pairing(charles, bob)), NOT(Pairing(charles, alice)))
                    )
                )
            SortingSatSolverService.variables.clear()
            SortingSatSolverService.variables +=
                mapOf(
                    1 to Pairing(alice, bob),
                    2 to Pairing(alice, charles),
                    3 to Pairing(bob, alice),
                    4 to Pairing(bob, charles),
                    5 to Pairing(charles, alice),
                    6 to Pairing(charles, bob),
                )

            val dimacsExpr = sortingService.toDimacs(expr)

            expectThat(dimacsExpr).containsExactlyInAnyOrder(
                listOf(
                    setOf(1, 2), setOf(-2, -1), setOf(3, 4), setOf(-4, -3), setOf(5, 6), setOf(-6, -5)
                )
            )
        }
    }
}

private fun checkNOTOnlyAtPrimitiveLevel(expr: LogicalExpression) {
    when (expr) {
        is NOT -> expectThat(expr.a).isA<Pairing>()
        is Pairing -> {}
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
        is NOT, is Pairing -> {}
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