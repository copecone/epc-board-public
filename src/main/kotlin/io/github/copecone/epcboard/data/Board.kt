package io.github.copecone.epcboard.data
import io.github.copecone.epcboard.api.APIAddressReader
import io.github.copecone.epcboard.data.event.BoardEvent
import io.github.copecone.epcboard.data.event.BoardEventFrame
import io.github.copecone.epcboard.data.event.client.Handshake
import io.github.copecone.epcboard.data.event.client.NewLink
import io.github.copecone.epcboard.data.event.room.LevelChange
import io.github.copecone.epcboard.data.event.room.PlayerAccuracyChange
import io.github.copecone.epcboard.data.event.room.PlayerJoin
import io.github.copecone.epcboard.data.event.room.PlayerLeave
import io.github.copecone.epcboard.util.SerializerUtil
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketException

private val scope = CoroutineScope(Dispatchers.Default)

object DataManager {
    @Suppress("DeferredResultUnused")
    fun getData(id: ULong): BoardConnection {
        return BoardConnection(id).takeIf { it.status == 0 }?.apply { startLoop() } ?: BoardConnection.connectionDataMap[id]!!
    }
}

class BoardConnection(val id: ULong) {
    var status: Int = 0
    var roomData: RoomInfo? = null
    var closed: Boolean = false

    var dataReqID = 0
    var reqCount: Int = 0
    private val client = HttpClient(CIO)

    companion object {
        val connectionIDSet = HashSet<ULong>()
        val connectionDataMap = HashMap<ULong, BoardConnection>()
    }

    @Suppress("LABEL_NAME_CLASH", "DeferredResultUnused")
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
                            val newData = Json.decodeFromString<RoomInfo>(text)

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

@Suppress("DeferredResultUnused")
@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
class Board(val name: String, val roomID: ULong) {
    var enabled = true
    val apiConnection: BoardConnection = DataManager.getData(roomID)
    val players = ArrayList<Long>()

    var roomDataCopy: RoomInfo? = null
    val eventQueue = ArrayDeque<BoardEvent>()
    val closedSessionQueue = ArrayDeque<DefaultWebSocketSession>()

    private val connections = ArrayList<DefaultWebSocketSession>()
    fun addConnection(session: DefaultWebSocketSession) = connections.add(session)

    init {
        CoroutineScope(Dispatchers.IO).async {
            try {
                while (enabled) {
                    for (session in connections) {
                        if (session.outgoing.isClosedForSend) {
                            session.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Outgoing is not enabled"))
                            closedSessionQueue.add(session)
                            continue
                        }

                        while (!session.incoming.isEmpty) {
                            val frame = session.incoming.receive()
                            frame as Frame.Text? ?: continue

                            val receivedText = frame.readText()
                            val eventFrame = SerializerUtil.jsonBuild.decodeFromString<BoardEventFrame>(receivedText)
                            when (eventFrame.event) {
                                is Handshake -> {
                                    scope.launch {
                                        while (apiConnection.roomData == null) delay(10)
                                        session.send(SerializerUtil.jsonBuild.encodeToString(BoardEventFrame(1, NewLink(apiConnection.roomData!!))))
                                    }
                                }
                            }
                        }
                    }

                    while (closedSessionQueue.isNotEmpty()) {
                        val session = closedSessionQueue.removeFirst()
                        connections.remove(session)
                    }

                    while (eventQueue.isNotEmpty()) {
                        val event = eventQueue.removeFirst()
                        for (session in connections) {
                            session.send(SerializerUtil.jsonBuild.encodeToString(BoardEventFrame(event.op, event)))
                        }
                    }

                    delay(20)
                }
            } catch (err: Exception) { println(err.printStackTrace()) }
        }

        CoroutineScope(Dispatchers.IO).async {
            while (true) {
                if (apiConnection.roomData != null) {
                    detectAnything()
                    roomDataCopy = apiConnection.roomData!!

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

    // This function detects anything changing in room
    private fun detectAnything() {
        if (roomDataCopy == null) return

        if (apiConnection.roomData!!.level.hash != roomDataCopy!!.level.hash) { // 레벨 변경
            eventQueue.add(LevelChange(apiConnection.roomData!!.level))
        }

        if (apiConnection.roomData!!.players != roomDataCopy!!.players) { // 플레이어 목록 변경
            val roomDataPlayerIDs = apiConnection.roomData!!.players.map { it.id }
            val roomDataCopyPlayerIDs = roomDataCopy!!.players.map { it.id }

            if (!roomDataCopyPlayerIDs.containsAll(roomDataPlayerIDs)) { // 플레이어 데이터 추가 (접속)
                val joinedPlayers = roomDataPlayerIDs.filter { player -> !roomDataCopyPlayerIDs.contains(player) }
                joinedPlayers.map { id -> apiConnection.roomData!!.players.first { it.id == id } }.forEach { player ->
                    eventQueue.add(PlayerJoin(player))
                }
            }

            if (!roomDataPlayerIDs.containsAll(roomDataCopyPlayerIDs)) { // 플레이어 데이터 제거 (나감)
                val leftPlayers = roomDataCopyPlayerIDs.filter { player -> !roomDataPlayerIDs.contains(player) }
                leftPlayers.forEach { player ->
                    eventQueue.add(PlayerLeave(player))
                }
            }

            if (apiConnection.roomData!!.isPlaying) { // 정확도 변경?!
                apiConnection.roomData!!.players.forEach { player ->
                    val playerID = player.id

                    if (!player.state.isSpectator) { // Check isn't spectator
                        val stateCopy = roomDataCopy!!.players.firstOrNull { it.id == playerID }?.state ?: return@forEach
                        if (!player.state.hitMarginsCount.contentEquals(stateCopy.hitMarginsCount)) {
                            eventQueue.add(PlayerAccuracyChange(player.id, player.state.hitMarginsCount, player.state.xAcc))
                        }
                    }
                }
            }
        }
    }
}