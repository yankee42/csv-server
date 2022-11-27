package com.github.yankee42

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.github.yankee42.plugins.*

fun main() {
    embeddedServer(Netty, port = 3001, host = "0.0.0.0") { module() }
        .start(wait = true)
}

fun Application.module() {
    configureRouting()
}
