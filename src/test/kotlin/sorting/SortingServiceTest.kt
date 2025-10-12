package sorting

import alice
import bob
import charles
import data.Person
import data.Request
import data.RequestType.GIFT_TO
import data.RequestType.NO_GIFT_TO
import diana
import edgar
import exceptions.UnsatisfiableConstraintsException
import florence
import george
import helen
import irene
import john
import logic.Pairing
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import sorting.satSolver.PeopleSortingSatSolverService
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class SolverServiceTest {

    val sortingService = PeopleSortingSatSolverService()

    @BeforeEach
    fun clearVariables() {
        PeopleSortingSatSolverService.pairings = emptyList()
    }

    @Nested
    inner class AssignPeopleOneRound {
        @RepeatedTest(100)
        fun `assign 3 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @RepeatedTest(100)
        fun `assign 4 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @RepeatedTest(20)
        fun `assign 5 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles, diana, edgar)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @RepeatedTest(20)
        fun `assign 6 people randomly without duplicates if no requests`() {
            val people = listOf(alice, bob, charles, diana, edgar, florence)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @RepeatedTest(100)
        fun `assign people randomly without duplicates if one GIFT_TO request`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = GIFT_TO, diana.id))
            )
            val people = listOf(aliceWithRequest, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @RepeatedTest(100)
        fun `assign people randomly without duplicates if one NO_GIFT_TO request`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val people = listOf(aliceWithRequest, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @RepeatedTest(20)
        fun `assign people randomly without duplicates if many requests`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val bobWithRequest = bob.copy(
                requests = listOf(Request(type = GIFT_TO, charles.id))
            )
            val charlesWithRequest = charles.copy(
                requests = listOf(Request(type = NO_GIFT_TO, edgar.id))
            )
            val people = listOf(aliceWithRequest, bobWithRequest, charlesWithRequest, diana, edgar, florence)
            val pairings = sortingService.assignPeople(people, 1)

            check(people, pairings, 1)
        }

        @Test
        fun `throw error if impossible to match all requests`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val bobWithRequest = bob.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val charlesWithRequest = charles.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val edgarWithRequest = edgar.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val florenceWithRequest = florence.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val people = listOf(
                aliceWithRequest,
                bobWithRequest,
                charlesWithRequest,
                diana,
                edgarWithRequest,
                florenceWithRequest
            )

            expectThrows<UnsatisfiableConstraintsException> { sortingService.assignPeople(people, 1) }
        }
    }

    @Nested
    inner class AssignPeopleSeveralRounds {
        @RepeatedTest(100)
        fun `assign 3 people randomly without duplicates if no requests, a person cannot give several gift to an other`() {
            val nbGiftsPerPerson = 2
            val people = listOf(alice, bob, charles)
            val pairings = sortingService.assignPeople(people, nbGiftsPerPerson)

            check(people, pairings, nbGiftsPerPerson)
        }

        // Change nb of repetitions but beware of SortingSatSolverService.MAX_COMPUTATION_TIME_MS
        @RepeatedTest(1)
        fun `assign 6 people randomly without duplicates if no requests, a person cannot give several gift to an other`() {
            val nbGiftsPerPerson = 3
            val people = listOf(alice, bob, charles, diana, edgar, florence)
            val pairings = sortingService.assignPeople(people, nbGiftsPerPerson)

            check(people, pairings, nbGiftsPerPerson)
        }

        @RepeatedTest(5)
        fun `assign 6 people randomly without duplicates if many requests, a person cannot give several gift to an other`() {
            val nbGiftsPerPerson = 4

            val aliceWithRequest = alice.copy(
                requests = listOf(
                    Request(type = GIFT_TO, bob.id),
                    Request(type = GIFT_TO, charles.id),
                    Request(type = GIFT_TO, diana.id)
                )
            )
            val bobWithRequest = bob.copy(
                requests = listOf(Request(type = GIFT_TO, alice.id))
            )
            val charlesWithRequest = charles.copy(
                requests = listOf(Request(type = NO_GIFT_TO, edgar.id))
            )
            val edgarWithRequest = edgar.copy(
                requests = listOf(Request(type = GIFT_TO, florence.id))
            )

            val people = listOf(aliceWithRequest, bobWithRequest, charlesWithRequest, diana, edgarWithRequest, florence)
            val pairings = sortingService.assignPeople(people, nbGiftsPerPerson)

            check(people, pairings, nbGiftsPerPerson)
        }

        @Test
        fun `if impossible to assign people randomly without duplicates over one round with many requests, then do it over several`() {
            // TODO find example
        }

        @RepeatedTest(100)
        fun `real-life case with 10 people and multiple GIFT_TO and NO_GIFT_TO conditions`() {
            val nbGiftsPerPerson = 3

            val bobWithRequest = bob.copy(
                requests = listOf(
                    Request(type = NO_GIFT_TO, charles.id),
                    Request(type = GIFT_TO, diana.id),
                )
            )
            val charlesWithRequest = charles.copy(
                requests = listOf(
                    Request(type = NO_GIFT_TO, bob.id),
                    Request(type = GIFT_TO, florence.id),
                    Request(type = GIFT_TO, john.id),
                )
            )
            val dianaWithRequest = diana.copy(
                requests = listOf(
                    Request(type = NO_GIFT_TO, edgar.id),
                )
            )
            val edgarWithRequest = edgar.copy(
                requests = listOf(
                    Request(type = NO_GIFT_TO, diana.id),
                )
            )
            val georgeWithRequest = george.copy(
                requests = listOf(
                    Request(type = NO_GIFT_TO, helen.id),
                    Request(type = GIFT_TO, irene.id),
                )
            )
            val helenWithRequest = helen.copy(
                requests = listOf(
                    Request(type = NO_GIFT_TO, george.id),
                    Request(type = GIFT_TO, john.id),
                )
            )
            val ireneWithRequest = irene.copy(
                requests = listOf(
                    Request(type = GIFT_TO, george.id),
                    Request(type = GIFT_TO, helen.id),
                )
            )
            val johnWithRequest = john.copy(
                requests = listOf(
                    Request(type = GIFT_TO, george.id),
                    Request(type = GIFT_TO, helen.id),
                )
            )

            val people = listOf(
                alice,
                bobWithRequest,
                charlesWithRequest,
                dianaWithRequest,
                edgarWithRequest,
                florence,
                georgeWithRequest,
                helenWithRequest,
                ireneWithRequest,
                johnWithRequest,
            )
            val pairings = sortingService.assignPeople(people, nbGiftsPerPerson)

            check(people, pairings, nbGiftsPerPerson)
        }
    }
}

private fun checkNoOnePairedWithItself(pairings: List<Pairing>) {
    pairings.forEach { pairing ->
        expectThat(pairing.person.id).isNotEqualTo(pairing.linkedPerson.id)
    }
}

private fun checkAllPeopleAppearXTimes(people: List<Person>, pairings: List<Pairing>, x: Int) {
    people.forEach { person ->
        pairings.filter { it.person.id == person.id }
            .let { matchingPeople -> expectThat(matchingPeople.size).isEqualTo(x) }
    }
}

private fun checkAllPeopleGiftXTimes(people: List<Person>, pairings: List<Pairing>, x: Int) {
    people.forEach { person ->
        pairings.filter { it.person.id == person.id }
            .let { matchingPeople -> expectThat(matchingPeople.size).isEqualTo(x) }
    }
}

private fun checkAllPeopleAreGiftedXTimes(people: List<Person>, pairings: List<Pairing>, x: Int) {
    people.forEach { person ->
        pairings.filter { it.linkedPerson.id == person.id }
            .let { matchingPeople -> expectThat(matchingPeople.size).isEqualTo(x) }
    }
}

private fun checkNoOneReceiveSeveralGiftsFromTheSamePerson(people: List<Person>, pairings: List<Pairing>) {
    people.forEach { person ->
        pairings.filter { it.linkedPerson.id == person.id }
            .groupingBy { it.person.id }
            .eachCount()
            .forEach { (_, nbGifts) -> expectThat(nbGifts).isEqualTo(1) }
    }
}

private fun checkRequests(people: List<Person>, pairings: List<Pairing>) {
    people.forEach { person ->
        person.requests.forEach { request ->
            pairings.filter { it.person.id == person.id }
                .filter { it.linkedPerson.id == request.otherPersonId }
                .let { matchingPeople ->
                    if (request.type == NO_GIFT_TO) {
                        expectThat(matchingPeople).isEmpty()
                    } else if (request.type == GIFT_TO) {
                        expectThat(matchingPeople.size).isEqualTo(1)
                    }
                }
        }
    }
}

private fun check(
    people: List<Person>,
    pairings: List<Pairing>,
    nbGiftsPerPerson: Int,
) {
    expectThat(pairings.size).isEqualTo(people.size * nbGiftsPerPerson)
    checkNoOnePairedWithItself(pairings)
    checkAllPeopleAppearXTimes(people, pairings, nbGiftsPerPerson)
    checkAllPeopleGiftXTimes(people, pairings, nbGiftsPerPerson)
    checkAllPeopleAreGiftedXTimes(people, pairings, nbGiftsPerPerson)
    checkRequests(people, pairings)
    if (nbGiftsPerPerson > 1) {
        checkNoOneReceiveSeveralGiftsFromTheSamePerson(people, pairings)
    }
}