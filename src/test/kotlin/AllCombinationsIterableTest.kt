import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class AllCombinationsIterableTest {

    @Test
    fun `not last element has next element`() {
        val iterable = AllCombinationsIterable(listOf(true, true, false, true, false))

        expectThat(iterable.hasNext()).isTrue()
    }

    @Test
    fun `last element has no next element`() {
        val iterable = AllCombinationsIterable(listOf(false, false, false, true, true))

        expectThat(iterable.hasNext()).isFalse()
    }

    @Test
    fun `get next element from not last element`() {
        val iterable = AllCombinationsIterable(listOf(true, true, false, true, false))

        expectThat(iterable.next()).isEqualTo(listOf(true, true, false, false, true))
    }

    @Test
    fun `get next element from not last element - true in last place`() {
        val iterable = AllCombinationsIterable(listOf(true, true, false, false, true))

        expectThat(iterable.next()).isEqualTo(listOf(true, false, true, true, false))
    }

    @Test
    fun `cannot get next element from last element`() {
        val iterable = AllCombinationsIterable(listOf(false, false, false, true, true))

        expectThrows<NoNextItemException> { iterable.next() }
    }

    @Test
    fun `call next() while hasNext() sorts the list - all true at the end`() {
        var currentIterableValue: List<Boolean> = listOf(true, true, true, false, false)
        val iterable = AllCombinationsIterable(currentIterableValue)

        while (iterable.hasNext()) {
            currentIterableValue = iterable.next()
        }

        expectThat(currentIterableValue).isEqualTo(listOf(false, false, true, true, true))
    }
}