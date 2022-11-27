package com.github.yankee42

fun <T : Comparable<T>> Sequence<T>.minMaxOrNull(): Pair<T, T>? {
    return fold<T, Pair<T, T>?>(null) { acc, item ->
        if (acc == null) {
            item to item
        } else {
            when {
                item < acc.first -> item to acc.second
                item > acc.second -> acc.first to item
                else -> acc
            }
        }
    }
}
