package io.github.copecone.epcboard.data
import io.github.copecone.epcboard.api.APIAddressReader
import io.github.copecone.epcboard.data.event.BoardEvent
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.net.SocketException

private val scope = CoroutineScope(Dispatchers.Default)

object DataManager {
    @Suppress("DeferredResultUnused")
    fun getData(id: ULong): BoardConnection {
        return (BoardConnection(id).takeIf { it.status == 0 } ?: BoardConnection.connectionDataMap[id]!!).apply { startLoop() }
    }
}

class BoardConnection(val id: ULong) {
    var status: Int = 0
    var roomData: RawRoomData? = null
    var closed: Boolean = false

    var dataReqID = 0
    var reqCount: Int = 0
    private val client = HttpClient(CIO)

    companion object {
        val connectionIDSet = HashSet<ULong>()
        val connectionDataMap = HashMap<ULong, BoardConnection>()
    }

    @Suppress("LABEL_NAME_CLASH")
    fun startLoop() = scope.async {
        while (true) {
            if (id != 0uL) {
                val currentReqID = reqCount++
                scope.async {
                    val response: HttpResponse
                    try {
                        try {
                            response = client.get("https://${APIAddressReader.address}/rooms/${id}")
                            if (response.status == HttpStatusCode.NotFound) {
                                closed = true; return@async
                            }

                            val text = response.body<String>()
                            val newData = Json.decodeFromString<RawRoomData>(text)

                            if (dataReqID <= currentReqID) {
                                dataReqID = currentReqID
                                roomData = newData
                            }
                        } catch (err: HttpRequestTimeoutException) {
                            println("HTTP Timeout Caused:\n${err.message}")
                        }
                    } catch (err: SocketException) {
                        println("HTTP SocketException Caused:\n${err.message}")
                    }
                }
            }

            delay(300)
        }
    }

    init {
        if (!connectionIDSet.contains(id)) {
            connectionIDSet.add(id)
            connectionDataMap[id] = this
        } else { status = 1 }
    }
}

@OptIn(DelicateCoroutinesApi::class)
class Board(val name: String, val roomID: ULong) {
    var enabled = true
    val apiConnection: BoardConnection = DataManager.getData(roomID)
    val players = ArrayList<Long>()

    val eventQueue = ArrayDeque<BoardEvent>()

    private val connections = ArrayList<DefaultWebSocketSession>()
    fun addConnection(session: DefaultWebSocketSession) = connections.add(session)

    init {
        scope.async {
            try {
                while (enabled) {
                    // TODO: Send board events to sessions
                    for (session in connections) {
                        if (!session.outgoing.isClosedForSend) session.send("Hi!")
                    }
                    delay(50)
                }
            } catch (err: Exception) { println(err.printStackTrace()) }
        }

        CoroutineScope(Dispatchers.IO).async {
            while (true) {
                // TODO: Catch any events happening on the room and push it into event queue
                if (apiConnection.roomData != null) {
                    for (player in apiConnection.roomData!!.players) {
                        print("\r" + " ".repeat(100) + "\r")
                        print("isSpectator: ${player.state.isSpectator} | XAcc: ${player.state.xAcc}, HitMargins: [${player.state.hitMarginsCount.joinToString(",")}] | ReqID: ${apiConnection.dataReqID}")
                    }
                }

                if (apiConnection.closed) { enabled = false; break }
                delay(20)
            }
        }
    }
}