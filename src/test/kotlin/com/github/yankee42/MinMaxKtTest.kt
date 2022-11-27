package com.github.yankee42

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class MinMaxKtTest {
    @Test(dataProvider = "provide_sequence_minMax")
    fun minMaxOrNull(sequence: Sequence<Int>, expectedPair: Pair<Int, Int>?) {
        // execution
        val minMax = sequence.minMaxOrNull()

        // evaluation
        assertThat(minMax, equalTo(expectedPair))
    }

    @DataProvider
    fun provide_sequence_minMax(): Array<Array<Any?>> = arrayOf(
        arrayOf(sequenceOf(1, 2, 3), 1 to 3),
        arrayOf(sequenceOf(1), 1 to 1),
        arrayOf(emptySequence<Int>(), null),
    )
}
