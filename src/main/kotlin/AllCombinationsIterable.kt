import java.util.*

data class AllCombinationsIterable(val list: List<Boolean>) : Iterator<List<Boolean>> {

    // At all times, there is exactly nbAssociations true values in the list, all others are false
    // The initial combination list is supposed to have all the true values at the beginning :
    // Ex with nbAssociations=2 : [ 1 1 0 0 0 ]

    private val currentCombination: List<Boolean> = list
    private val nbAssociations: Int = list.count { it } // Count true values

    // There is no next combination if all the true values are at the end of the list
    // Ex with nbAssociations=2 : [ 0 0 0 1 1 ]
    override fun hasNext(): Boolean {
        val lastIndex = currentCombination.size - 1

        val lastNbAssociationsValues = currentCombination.slice(lastIndex - nbAssociations + 1..lastIndex)

        return !lastNbAssociationsValues.all { it }
    }

    /** Compute the next combination :
     * 1) Take the true value closest to the end
     * 2) Exchange its position with the next value if it is false
     * 3) If 2) is impossible, then find the previous true value and go to 2)
     * 4) If 2) was done, then move all true values which are between the one moved and the end of the list.
     *    Place them directly after the moved value and replace their previous spots with false values.
     * 5) If no true value can be moved, then hasNext() should have been false --> stop
     */
    // Ex with nbAssociations=2, currentCombination=[ 1 0 1 0 0 ] --> next --> [ 1 0 0 1 0 ]
    // Ex with nbAssociations=2, currentCombination=[ 1 0 0 0 1 ] --> next --> [ 0 1 0 0 1 ] after 3) --> [ 0 1 1 0 0 ] after 4)
    override fun next(): List<Boolean> {
        if (!hasNext()) throw NoNextItemException()

        for (i in currentCombination.size - 1 downTo 0) {
            if (currentCombination[i]) {
                // list at index i is true

                if (i < currentCombination.size - 1 && !currentCombination[i + 1]) {
                    // i is not last index and list at index i+1 is false

                    Collections.swap(currentCombination, i, i + 1)

                    // For all true values > i+1, move them at indexes following i+1
                    var nextAvailableSpace = i + 2
                    for (j in i + 2..<currentCombination.size) {
                        if (currentCombination[j] && j > nextAvailableSpace) {
                            Collections.swap(currentCombination, j, nextAvailableSpace)
                            nextAvailableSpace++
                        }
                    }

                    return currentCombination
                }
            }
        }

        // This state should not be reachable
        throw IllegalStateException("If hasNext()==true, AllCombinationsIterable should have a next item")
    }

}

class NoNextItemException : Exception()
