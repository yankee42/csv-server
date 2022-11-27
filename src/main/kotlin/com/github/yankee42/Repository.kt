package com.github.yankee42

import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlin.io.path.exists
import kotlin.io.path.useDirectoryEntries
import kotlin.io.path.useLines

private val csvFileNameFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral('-')
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral(".csv")
    .toFormatter()

class Repository(private val baseDir: Path) {
    fun getRows(room: String, type: String, start: Instant, stop: Instant): Sequence<TimeRow> {
        val typeDir = baseDir.resolve(room).resolve(type)
        val (minYear, maxYear) = getMinMaxYear(typeDir) ?: return emptySequence()
        val startDay = LocalDate.ofInstant(start, ZoneOffset.UTC).let {
            if (it.year < minYear) {
                getFirstDate(typeDir, minYear) ?: return emptySequence()
            } else {
                it!!
            }
        }
        val timeRowIterator = generateSequence(startDay) { it.plusDays(1) }
            .takeWhile { it.year <= maxYear }
            .map { typeDir.resolveCsvForDate(it) }
            .filter { it.exists() }
            .flatMap { it.linesSkippingHeader() }
            .map { TimeRow(it) }
            .iterator()
        var peek1 = timeRowIterator.nextOr { return emptySequence() }
        if (peek1.instant > start) {
            getLastLineBefore(typeDir, startDay)?.let { peek1 = TimeRow(it) }
        }
        if (peek1.instant >= stop) {
            if (peek1.instant == stop) {
                return sequenceOf(peek1)
            }
            return emptySequence()
        }
        var peek2: TimeRow
        while (true) {
            peek2 = timeRowIterator.nextOr { return if (peek1.instant >= start) sequenceOf(peek1) else emptySequence() }
            if (peek2.instant > start) {
                break
            }
            peek1 = peek2
        }
        if (peek2.instant >= stop) {
            if (peek1.instant == stop) {
                return sequenceOf(peek1)
            }
            return sequenceOf(peek1, peek2)
        }
        return sequence {
            yield(peek1)
            yield(peek2)
            while (timeRowIterator.hasNext()) {
                val next = timeRowIterator.next()
                yield(next)
                if (next.instant >= stop) {
                    return@sequence
                }
            }
        }
    }
}

private fun getMinMaxYear(dir: Path): Pair<Int, Int>? =
    dir.useDirectoryEntries { pathSequence ->
        pathSequence
            .map { it.fileName.toString().toInt() }
            .minMaxOrNull()
    }

private fun getFirstDate(typeDir: Path, year: Int): LocalDate? = typeDir.useDirectoryEntries { pathSequence ->
    pathSequence.minOfOrNull {
        val name = it.fileName.toString()
        val dashPos = name.indexOf('-')
        val dotPos = name.indexOf('.', dashPos + 1)
        LocalDate.of(year, name.substring(0, dashPos).toInt(), name.substring(dashPos + 1, dotPos).toInt())
    }
}

private fun Path.resolveCsvForDate(date: LocalDate): Path =
    resolve(date.year.toString()).resolve(csvFileNameFormatter.format(date))

private fun getLastLineBefore(typeDir: Path, date: LocalDate): String? {
    typeDir.resolveCsvForDate(date.minusDays(1)).let { previousCsv ->
        if (previousCsv.exists()) {
            return previousCsv.useLines { it.last() }
        }
    }
    return null
}

private inline fun <T> Iterator<T>.nextOr(or: () -> T): T = if (hasNext()) next() else or()

private fun Path.linesSkippingHeader(): Sequence<String> = sequence { useLines { yieldAll(it.drop(1)) } }
