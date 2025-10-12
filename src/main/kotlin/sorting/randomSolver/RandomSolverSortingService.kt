package sorting.randomSolver

import data.Person
import data.Request
import data.RequestType
import data.RequestType.GIFT_TO
import data.RequestType.NO_GIFT_TO
import exceptions.UnsatisfiableConstraintsException
import logic.Pairing
import sorting.SolverService

class RandomSolverSortingService : SolverService() {

    /**
     * The random solver is a naive implementation of a sat solver :
     * - satisfies the GIFT_TO constraints first
     * - pick the remaining gifts at random while making sure they satisfy the NO_GIFT_TO constraints
     * - if it is not possible to satisfy all the constraints, go back to a previous state and pick another person
     * - continue until a satisfactory solution is found or the timeout is reached
     */
    // TODO add timeout
    override fun assignPeople(people: List<Person>, nbGiftsPerPerson: Int): List<Pairing> {
        checkConstraints(people, nbGiftsPerPerson)

        // For each person, associate the list of pairings. Initialize it either with the GIFT_TO constraints if they
        //  exist or with an empty list.
        val peoplePairings: List<Pair<Person, MutableList<Pairing>>> =
            people
                .shuffled()
                .map { person ->
                    val pairings = mutableListOf<Pairing>()
                    pairings.addAll(
                        buildGiftToPairings(person = person, people = people, nbGiftsPerPerson = nbGiftsPerPerson)
                    )
                    person to pairings
                }

        var completePeoplePairings: List<Pair<Person, List<Pairing>>>? = null
        while (completePeoplePairings == null) { // TODO add timeout check
            // If the previous iteration of `pickPairings()` was not successful, then try again. Maybe another random
            //  picking order will work ?
            // NB : If there is only a small number of solutions and the probability of finding one of them at
            //  random is low, then it is possible that no solution is found within the time limit
            completePeoplePairings = pickPairings(people, peoplePairings, nbGiftsPerPerson)
        }

        return completePeoplePairings.map { it.second }.flatten()
    }

    /************************************************************************************************************/

    private fun buildGiftToPairings(person: Person, people: List<Person>, nbGiftsPerPerson: Int): List<Pairing> =
        person.requests
            .keepOnlyTypeRequests(GIFT_TO)
            .let { requests -> requests.map { request -> buildPairing(person, request, people) } }

    private fun List<Request>.keepOnlyTypeRequests(type: RequestType): List<Request> =
        filter { request -> request.type == type }

    private fun buildPairing(person: Person, request: Request, people: List<Person>): Pairing =
        Pairing(
            person = person,
            linkedPerson = people.findPerson(request.otherPersonId)
        )

    /**
     * Look at the initial constraints to see if they seem possible to satisfy
     * - One person cannot have more GIFT_TO than gifts given
     * - One person cannot have NO_GIFT_TO constraints from all the others
     */
    private fun checkConstraints(people: List<Person>, nbGiftsPerPerson: Int) {
        // One person cannot have more GIFT_TO than gifts given
        people.forEach { person ->
            person.requests
                .keepOnlyTypeRequests(GIFT_TO)
                .let { giftToRequests ->
                    if (giftToRequests.size > nbGiftsPerPerson) throw UnsatisfiableConstraintsException()
                }
        }

        // One person cannot have NO_GIFT_TO constraints from all the others
        /* TODO people.forEach { person ->
            people.map { it.requests.keepOnlyTypeRequests(NO_GIFT_TO) }
                .filter { request -> TODO() }
        }*/

        // One person cannot have too many NO_GIFT_TO constraints
        // The following equation must always be respected
        // people.size - nb NO_GIFT_TO <= nbGiftsPerPerson
        // TODO
    }

    // Pick one person at random from the people list and add the correct number of pairings at random
    private fun pickPairings(
        people: List<Person>,
        peoplePairings: List<Pair<Person, List<Pairing>>>,
        nbGiftsPerPerson: Int,
    ): List<Pair<Person, MutableList<Pairing>>>? {
        val mutablePeoplePairings = peoplePairings.map { it.first to it.second.toMutableList() }

        mutablePeoplePairings.forEach { (person, pairings) ->
            val poolOfPersonsToGift: MutableList<Person> = initializePersonPool(
                people = people,
                person = person,
                pairings = pairings,
                peoplePairings = mutablePeoplePairings,
                nbGiftsPerPerson = nbGiftsPerPerson
            )

            while (pairings.size < nbGiftsPerPerson) {
                if (poolOfPersonsToGift.isEmpty()) {
                    // Stop the loop, we are in an insolvable scenario
                    return null
                } else {
                    poolOfPersonsToGift.random()
                        .let { pickedPerson ->
                            pairings.add(Pairing(person, pickedPerson))
                            poolOfPersonsToGift.remove(pickedPerson)
                        }
                }
            }
        }

        return mutablePeoplePairings
    }

    /**
     * The pool of people a person can gift to is composed of all the people, from which we remove
     * - the person itself
     * - the ones which the person already gifts to
     * - the ones with the NO_GIFT_TO request
     * - the people who have already received enough gifts
     */
    private fun initializePersonPool(
        people: List<Person>,
        person: Person,
        pairings: List<Pairing>,
        peoplePairings: List<Pair<Person, List<Pairing>>>,
        nbGiftsPerPerson: Int,
    ): MutableList<Person> =
        people.minus(person)
            .minus(pairings.map { it.linkedPerson }.toSet()) // Already gifts to
            .minus(people.keepOnlyNoGiftToRequests(person)) // Must not gift to
            .minus(peoplePairings.keepOnlyAllGiftsReceived(nbGiftsPerPerson)) // Have already received enough gifts
            .toMutableList()

    private fun List<Person>.keepOnlyNoGiftToRequests(person: Person): Set<Person> =
        person.requests
            .keepOnlyTypeRequests(NO_GIFT_TO)
            .map { noGiftToRequest -> noGiftToRequest.otherPersonId }
            .let { noGiftToPersonIdList -> this.filter { it.id in noGiftToPersonIdList } }
            .toSet()

    private fun List<Pair<Person, List<Pairing>>>.keepOnlyAllGiftsReceived(nbGiftsPerPerson: Int): Set<Person> =
        flatMap { it.second }
            .map { pairing -> pairing.linkedPerson }
            .groupBy { it.id }
            .filter { (id, linkedPersonAppearances) -> linkedPersonAppearances.size >= nbGiftsPerPerson }
            .map { (id, linkedPersonAppearances) -> linkedPersonAppearances.first() }
            .toSet()
}