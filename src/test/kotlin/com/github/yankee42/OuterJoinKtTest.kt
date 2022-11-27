package com.github.yankee42

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class OuterJoinKtTest {
    @Test(dataProvider = "provide_lists_expectedResult")
    fun outerJoin(lists: List<List<Int>>, expected: String) {
        // execution
        val actual = outerJoin(lists.map { it.iterator() }).toList()

        // evaluation
        assertThat(actual.toString(), equalTo(expected))
    }

    @DataProvider
    fun provide_lists_expectedResult() = arrayOf(
        // join two lists:
        arrayOf(listOf(emptyList<Int>(), emptyList()), "[]"),
        arrayOf(listOf(listOf(1), listOf(2)), "[[1, null], [null, 2]]"),
        arrayOf(listOf(listOf(1), listOf(1, 2)), "[[1, 1], [null, 2]]"),
        arrayOf(listOf(listOf(1, 2), listOf(1)), "[[1, 1], [2, null]]"),
        arrayOf(listOf(listOf(1, 2), listOf(1, 2)), "[[1, 1], [2, 2]]"),

        // join three lists:
        arrayOf(listOf(emptyList<Int>(), emptyList(), emptyList()), "[]"),
        arrayOf(listOf(listOf(1), listOf(2), listOf(3)), "[[1, null, null], [null, 2, null], [null, null, 3]]"),
        arrayOf(listOf(listOf(1), listOf(1), listOf(1)), "[[1, 1, 1]]"),
    )
}
