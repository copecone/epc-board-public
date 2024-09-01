package io.github.copecone.epcboard

import io.github.copecone.epcboard.api.APIAddressReader
import io.github.copecone.epcboard.data.Board
import io.github.copecone.epcboard.data.BoardData
import io.github.copecone.epcboard.util.SerializerUtil
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.HashMap
import kotlin.time.Duration.Companion.seconds

val boards = HashMap<Int, Board>()
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

            route("/board") {
                webSocket("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: return@webSocket call.respond(HttpStatusCode.BadRequest)
                    boards[id]?.addConnection(this) ?: return@webSocket call.respond(HttpStatusCode.NotFound)

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

                get("/{id}") {
                    val id = call.parameters["id"]?.toInt() ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val board = boards[id] ?: return@get call.respond(HttpStatusCode.NotFound)

                    val boardData = BoardData(board.roomID.toString(), board.name, "/board/${id}")
                    call.respondText(SerializerUtil.jsonBuild.encodeToString(boardData), ContentType.Application.Json)
                }

                post("/create") {
                    try {
                        val data = SerializerUtil.jsonBuild.decodeFromString<JsonObject>(call.receiveText())

                        val boardName =
                            data["boardName"]?.jsonPrimitive ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val roomID =
                            data["roomID"]?.jsonPrimitive?.content?.toULongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)

                        val id = generateSequence { (0..Int.MAX_VALUE).random() }.first { !boards.containsKey(it) }
                        val board = Board(boardName.content, roomID)

                        boards[id] = board
                        val boardData = BoardData(board.roomID.toString(), board.name, "/board/${id}")
                        call.respondText(SerializerUtil.jsonBuild.encodeToString(boardData), ContentType.Application.Json)
                    } catch (err: Exception) {
                        println(err.stackTraceToString())
                    }
                }
            }
        }
    }.start(wait = true)
}