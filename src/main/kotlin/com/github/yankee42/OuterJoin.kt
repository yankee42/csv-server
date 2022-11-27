package com.github.yankee42

fun <T : Comparable<T>> outerJoin(iterators: List<Iterator<T>>): Sequence<List<T?>> =
    outerJoin(iterators, Comparator.naturalOrder())

fun <T> outerJoin(iterators: List<Iterator<T>>, comparator: Comparator<T>): Sequence<List<T?>> {
    val comparatorNullsLast = Comparator.nullsLast(comparator)
    val values = iterators.mapTo(ArrayList(iterators.size)) { it.nextOrNull() }

    return sequence {
        while (true) {
            val min: T? = values.minWith(comparatorNullsLast) ?: return@sequence
            val tmp = values.mapIndexed { index, it ->
                if (comparatorNullsLast.compare(min, it) == 0) {
                    values[index] = iterators[index].nextOrNull()
                    it
                } else {
                    null
                }
            }
            yield(tmp)
        }
    }
}

private fun <T> Iterator<T>.nextOrNull() = if (hasNext()) next() else null
