package logic

import SortingSatSolverService
import alice
import bob
import charles
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

class DimacsTest {
    val sortingService = SortingSatSolverService()

    @BeforeEach
    fun clearVariables(){
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

            val dimacsExpr = sortingService.toDimacs(expr)

            expectThat(dimacsExpr).containsExactlyInAnyOrder(
                listOf(
                    setOf(1, 2), setOf(-2, -1), setOf(3, 4), setOf(-4, -3), setOf(5, 6), setOf(-6, -5)
                )
            )
        }
    }
}