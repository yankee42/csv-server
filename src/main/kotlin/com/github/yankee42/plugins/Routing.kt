package com.github.yankee42.plugins

import com.github.yankee42.Repository
import com.github.yankee42.TimeRow
import com.github.yankee42.outerJoin
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.LoggerFactory
import java.io.Writer
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.listDirectoryEntries

private val REPOSITORY_DIR = Paths.get("sensors")
private val ROOMS = REPOSITORY_DIR.listDirectoryEntries().map { it.fileName.toString() }
private val MEASUREMENTS =
    ROOMS.asSequence()
        .flatMap { REPOSITORY_DIR.resolve(it).listDirectoryEntries() }
        .map { it.fileName.toString() }
        .toSet()
private val COMPARE_INSTANT: Comparator<TimeRow?> = Comparator.comparing { it?.instant }
private val log = LoggerFactory.getLogger("Routing")
private val repository = Repository(Paths.get("sensors"))

fun Application.configureRouting() {
    log.info("Configuring routing. Available rooms={}, available measurements={}", ROOMS, MEASUREMENTS)
    routing {
        MEASUREMENTS.forEach {
            get("/${it}", csvHandler(it))
        }
    }
}

private fun csvHandler(measurement: String): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    val from = Instant.ofEpochMilli(call.parameters.getOrFail<Long>("from"))
    val to = Instant.ofEpochMilli(call.parameters.getOrFail<Long>("to"))
    log.info("Processing request from={}, to={}", from, to)

    call.respondTextWriter { writeCsv(measurement, from, to) }
}

private fun Writer.writeCsv(type: String, from: Instant, to: Instant) {
    ROOMS.joinTo(this, prefix = "time,", separator = ",", postfix = "\n")
    outerJoin(ROOMS.map { repository.getRows(it, type, from, to).iterator() }, COMPARE_INSTANT)
        .forEachIndexed { index, row ->
            if (index != 0) {
                append('\n')
            }
            append(row.filterNotNull().first().raw.substringBefore(','))
            row.forEach {
                append(',')
                append(it?.raw?.substringAfter(','))
            }
        }
}
