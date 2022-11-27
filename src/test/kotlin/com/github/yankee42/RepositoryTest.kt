package com.github.yankee42

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.nio.file.FileSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText

class TestFile(val rows: List<TestFileRow>) {
    val forDate = LocalDate.ofInstant(rows[0].instant, ZoneOffset.UTC)
}
class TestFileRow(val raw: String) {
    val instant = Instant.parse(raw.substringBefore(','))
}

private val TEST_FILES = arrayOf(arrayOf(
    "2010-06-10T20:00:00.000Z,0.0",
    "2010-06-10T20:00:00.005Z,0.1",
), arrayOf(
    "2010-06-11T05:06:41.424Z,1.0",
    "2010-06-11T07:06:41.424Z,1.1",
    "2010-06-11T07:08:41.424Z,1.2",
    "2010-06-11T08:06:41.424Z,1.3",
    "2010-06-11T09:06:41.424Z,1.4",
))
    .map { lines -> TestFile(lines.map { line -> TestFileRow(line) }) }

private val csvFileNameFormatter = DateTimeFormatterBuilder()
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral('-')
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral(".csv")
    .toFormatter()

private const val ROOM = "someRoom"
private const val TYPE = "someType"

class RepositoryTest {
    private lateinit var fileSystem: FileSystem
    private lateinit var repository: Repository

    @BeforeMethod
    fun setUp() {
        fileSystem = MemoryFileSystemBuilder.newEmpty().build()
        val typeDir = fileSystem.getPath(ROOM, TYPE)
        typeDir.createDirectories()
        TEST_FILES.asSequence().map { it.forDate.year }.distinct().forEach {
            typeDir.resolve(it.toString()).createDirectory()
        }
        TEST_FILES.forEach { testFile ->
            typeDir
                .resolve(testFile.forDate.year.toString())
                .resolve(csvFileNameFormatter.format(testFile.forDate))
                .writeText(testFile.rows.joinToString("\n", "time,value\n") { it.raw })
        }
        repository = Repository(fileSystem.getPath("/"))
    }

    @AfterMethod
    fun tearDown() {
        fileSystem.close()
    }

    @Test(dataProvider = "provide_selectedRows")
    fun getRows_exactlyMatchingTimestamps(_name: String, selectedRows: List<TestFileRow>) {
        // execution
        val actual = repository.getRows(ROOM, TYPE, selectedRows[0].instant, selectedRows.last().instant)
            .joinToString("\n") { it.raw }

        // evaluation
        assertThat(actual, equalTo(selectedRows.joinToString("\n") { it.raw }))
    }

    @DataProvider
    fun provide_selectedRows() = arrayOf(
        // single file
        arrayOf("full file", TEST_FILES[0].rows),
        arrayOf("part of file beginning", TEST_FILES[1].rows.take(2)),
        arrayOf("part of file middle", TEST_FILES[1].rows.drop(1).dropLast(1)),
        arrayOf("part of file end", TEST_FILES[1].rows.takeLast(2)),

        // multi file
        arrayOf("one record from each file", listOf(TEST_FILES[0].rows.last(), TEST_FILES[1].rows.first())),
        arrayOf("all records from two files", TEST_FILES[0].rows + TEST_FILES[1].rows),
    )

    @Test(dataProvider = "provide_selectedRows")
    fun getRows_returnsRowBeforeNonMatch(_name: String, selectedRows: List<TestFileRow>) {
        // execution
        val actual = repository
            .getRows(ROOM, TYPE, selectedRows[0].instant.plusMillis(1), selectedRows.last().instant)
            .joinToString("\n") { it.raw }

        // evaluation
        assertThat(actual, equalTo(selectedRows.joinToString("\n") { it.raw }))
    }

    @Test(dataProvider = "provide_selectedRows")
    fun getRows_returnsRowAfterNonMatch(_name: String, selectedRows: List<TestFileRow>) {
        // execution
        val actual = repository
            .getRows(ROOM, TYPE, selectedRows[0].instant, selectedRows.last().instant.minusMillis(1))
            .joinToString("\n") { it.raw }

        // evaluation
        assertThat(actual, equalTo(selectedRows.joinToString("\n") { it.raw }))
    }

    @Test(dataProvider = "provide_referenceTime_emptySequence")
    fun getRows_returnsEmptySequence(_name: String, referenceTime: Instant) {
        // execution
        val actual = repository
            .getRows(ROOM, TYPE, referenceTime, referenceTime.plusMillis(1))
            .joinToString("\n") { it.raw }

        // evaluation
        assertThat(actual, equalTo(""))
    }

    @DataProvider
    fun provide_referenceTime_emptySequence() = arrayOf(
        arrayOf("before first", TEST_FILES[0].rows[0].instant.minusMillis(2)),
        arrayOf("after last", TEST_FILES.last().rows.last().instant.plusMillis(1)),
    )

    @Test(dataProvider = "provide_row_offsets_singleElement")
    fun getRows_returnsSinglePoint_ifExactMatch(_name: String, selectedRow: TestFileRow) {
        // execution
        val actual = repository
            .getRows(ROOM, TYPE, selectedRow.instant, selectedRow.instant)
            .joinToString("\n") { it.raw }

        // evaluation
        assertThat(actual, equalTo(selectedRow.raw))
    }

    @DataProvider
    fun provide_row_offsets_singleElement() = arrayOf(
        arrayOf("select first", TEST_FILES[0].rows[0]),
        arrayOf("select middle", TEST_FILES[1].rows[1]),
        arrayOf("select last", TEST_FILES.last().rows.last()),
    )

    @Test
    fun getRows_returnsEmptySequence_ifNoDataInDirectory() {
        // setup
        val typeDir = fileSystem.getPath(ROOM, "typeWithNoData")
        typeDir.createDirectory()
        val someInstant = Instant.ofEpochMilli(0)

        // execution
        val actual = repository.getRows(ROOM, "typeWithNoData", someInstant, someInstant).toList()

        // evaluation
        assertThat(actual, hasSize(0))
    }
}
