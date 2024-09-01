package io.github.copecone.epcboard

import io.github.copecone.epcboard.api.APIAddressReader
import io.github.copecone.epcboard.data.Board
import io.github.copecone.epcboard.data.BoardData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

val boards = ArrayList<Board>()
val client = HttpClient(CIO)

@OptIn(DelicateCoroutinesApi::class)
fun main()  {
    embeddedServer(Netty, port = 8000) {
        install(WebSockets) {
            pingPeriod = java.time.Duration.ofSeconds(15)
            timeout = java.time.Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/board/{id}") {
                val id = call.parameters["id"]?.toInt() ?: return@webSocket call.respond(HttpStatusCode.BadRequest)

                boards[id].addConnection(this)

                var initialTime = Clock.System.now()
                while (true) {
                    delay(1)
                    val currentTime = Clock.System.now()
                    if (currentTime - initialTime > 5.seconds) {
                        if (!outgoing.isClosedForSend) outgoing.send(Frame.Text("Heartbeat"))
                        else break

                        initialTime += 5.seconds
                    }
                }
            }

            get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    mainDashboard()
                }
            }

            get("/api-proxy/{path...}") {
                val path = call.parameters["path"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val response = client.get("https://${APIAddressReader.address}/$path")

                call.respondBytes(response.body<ByteArray>(), response.contentType(), response.status)
            }

            get("/scripts/{path...}") {
                val path = call.parameters.getAll("path")!!.joinToString("/")

                val scriptStream = javaClass.getResourceAsStream("/scripts/$path")
                if (scriptStream == null) { call.respond(HttpStatusCode.NotFound); return@get }

                val reader = scriptStream.bufferedReader(Charsets.UTF_8)
                call.respondText(reader.readText(), ContentType.Text.JavaScript)
            }

            post("/board/create") {
                try {
                    val data = Json.decodeFromString<JsonObject>(call.receiveText())

                    val boardName =
                        data["boardName"]?.jsonPrimitive ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val roomID =
                        data["roomID"]?.jsonPrimitive?.content?.toULong() ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val board = Board(boardName.content, roomID)

                    boards.add(board)

                    val boardData = BoardData(board.roomID.toString(), board.name, "/board/${boards.size - 1}")
                    call.respondText(Json.encodeToString(boardData), ContentType.Application.Json)
                } catch (err: Exception) {
                    println(err.stackTraceToString())
                }
            }
        }
    }.start(wait = true)
}