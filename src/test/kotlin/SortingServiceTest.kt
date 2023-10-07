import data.Person
import data.Request
import data.RequestType.GIFT_BY
import data.RequestType.GIFT_TO
import logic.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import kotlin.test.assertIsNot

class SortingServiceTest {

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
            checkNoANDinsideORs(exprCNF)
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

    @Nested
    inner class AssignPeopleOneRound {
        @RepeatedTest(100)
        fun `assign 3 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
        }

        @RepeatedTest(100)
        fun `assign 4 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
        }

        @RepeatedTest(100)
        fun `assign 5 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles, diana, edgar)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
        }

        @RepeatedTest(10)
        fun `assign 6 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles, diana, edgar, florence)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
        }

        @RepeatedTest(100)
        fun `assign people randomly without duplicates if one GIFT_TO request`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = GIFT_TO, diana.id))
            )
            val people = listOf(aliceWithRequest, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
            // Check that the request is satisfied
            pairings.first { it.person == aliceWithRequest }
                .let { alicePairing -> expectThat(alicePairing.linkedPerson).isEqualTo(diana) }

        }

        @RepeatedTest(100)
        fun `assign people randomly without duplicates if one GIFT_FROM request`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = GIFT_BY, diana.id))
            )
            val people = listOf(aliceWithRequest, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
            // Check that the request is satisfied
            pairings.first { it.linkedPerson == aliceWithRequest }
                .let { alicePairing -> expectThat(alicePairing.person).isEqualTo(diana) }
        }

        @Test
        fun `assign people randomly without duplicates if many requests`() {

        }

        @Test
        fun `throw error if impossible to match all requests`() {

        }
    }

    @Nested
    inner class AssignPeopleSeveralRounds {
        @Test
        fun `assign people randomly without duplicates if no requests, a person cannot give several gift to an other`() {

        }

        @Test
        fun `if impossible to assign people randomly without duplicates over one round with many requests, then do it over several`() {

        }
    }

    companion object {
        val alice = Person(
            id = "alice",
            firstName = "Alice",
            lastName = "Liddell",
            email = "alice@test.com",
            requests = emptyList()
        )
        val bob = Person(
            id = "bob",
            firstName = "Bob",
            lastName = "Ross",
            email = "bob@test.com",
            requests = emptyList()
        )
        val charles = Person(
            id = "charles",
            firstName = "Charles",
            lastName = "Dickens",
            email = "charles@test.com",
            requests = emptyList()
        )
        val diana = Person(
            id = "diana",
            firstName = "Lady",
            lastName = "Di",
            email = "diana@test.com",
            requests = emptyList()
        )
        val edgar = Person(
            id = "edgar",
            firstName = "Edgar",
            lastName = "E. Poe",
            email = "edgar@test.com",
            requests = emptyList()
        )
        val florence = Person(
            id = "florence",
            firstName = "Florence",
            lastName = "Nightingale",
            email = "florence@test.com",
            requests = emptyList()
        )
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

private fun checkNoANDinsideORs(expr: LogicalExpression) {
    when (expr) {
        is NOT, is Pairing -> {}
        is OR -> {
            assertIsNot<AND>(expr.a)
            assertIsNot<AND>(expr.b)
            checkNoANDinsideORs(expr.a)
            checkNoANDinsideORs(expr.b)
        }

        is AND -> {
            checkNoANDinsideORs(expr.a)
            checkNoANDinsideORs(expr.b)
        }

        else -> throw IllegalArgumentException("Illegal LogicalExpression in a CNF : $expr")
    }
}

private fun checkPersonNotPairedWithItself(pairing: Pairing) {
    expectThat(pairing.person.id).isNotEqualTo(pairing.linkedPerson.id)
}

private fun checkAllPeopleAppearOnce(people: List<Person>, pairings: List<Pairing>) {
    people.forEach { person ->
        pairings.filter { it.person.id == person.id }
            .let { matchingPeople -> expectThat(matchingPeople.size).isEqualTo(1) }
    }
}

private fun checkAllPeopleAreGiftedOnce(people: List<Person>, pairings: List<Pairing>) {
    people.forEach { person ->
        pairings.filter { it.linkedPerson.id == person.id }
            .let { matchingPeople -> expectThat(matchingPeople.size).isEqualTo(1) }
    }
}