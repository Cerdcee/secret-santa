import data.Person
import data.Request
import data.RequestType.GIFT_TO
import data.RequestType.NO_GIFT_TO
import logic.Pairing
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class PeopleSortingServiceTest {

    val sortingService = PeopleSortingService()

    @BeforeEach
    fun clearVariables(){
        PeopleSortingService.pairings = emptyList()
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
        fun `assign people randomly without duplicates if one NO_GIFT_TO request`() {
            val aliceWithRequest = alice.copy(
                requests = listOf(Request(type = NO_GIFT_TO, diana.id))
            )
            val people = listOf(aliceWithRequest, bob, charles, diana)
            val pairings = sortingService.assignPeople(people, 1)

            expectThat(pairings.size).isEqualTo(people.size)
            pairings.forEach { checkPersonNotPairedWithItself(it) }
            checkAllPeopleAppearOnce(people, pairings)
            checkAllPeopleAreGiftedOnce(people, pairings)
            // Check that the request is satisfied
            pairings.first { it.person == aliceWithRequest }
                .let { alicePairing -> expectThat(alicePairing.linkedPerson).isNotEqualTo(diana) }
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