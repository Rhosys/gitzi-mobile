package ch.rhosys.gitzi.data.remote

import ch.rhosys.gitzi.data.remote.dto.ChatMessageDto
import ch.rhosys.gitzi.data.remote.dto.ConfigDto
import ch.rhosys.gitzi.data.remote.dto.EpicDto
import ch.rhosys.gitzi.data.remote.dto.ReviewItemDto
import ch.rhosys.gitzi.data.remote.dto.TaskDto
import ch.rhosys.gitzi.di.SocketHttpClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject

/**
 * Live event stream from `GET /v1/events` (upgraded to a WebSocket). Every
 * message is a full snapshot of one entity type — the server-side projection
 * (board, review queue, chat, config) is rebuilt and re-broadcast the same
 * way the daemon rebuilds `KanbanBoard` from disk on every change, so the
 * client never has to reconcile incremental diffs.
 *
 * Wire shape: `{"type": "tasks", "tasks": [...]}`, `{"type": "epics", ...}`, etc.
 */
sealed interface GitziWireEvent {
    data class Epics(val epics: List<EpicDto>) : GitziWireEvent

    data class Tasks(val tasks: List<TaskDto>) : GitziWireEvent

    data class ReviewQueue(val items: List<ReviewItemDto>) : GitziWireEvent

    data class Chat(val messages: List<ChatMessageDto>) : GitziWireEvent

    data class Config(val config: ConfigDto) : GitziWireEvent
}

class GitziEventSocket
    @Inject
    constructor(
        @SocketHttpClient private val okHttpClient: OkHttpClient,
        private val json: Json,
    ) {
        fun connect(baseUrl: String): Flow<GitziWireEvent> =
            callbackFlow {
                val wsUrl = baseUrl.replaceFirst("http", "ws").trimEnd('/') + "/v1/events"
                val request =
                    Request.Builder()
                        .url(wsUrl)
                        .build()

                val listener =
                    object : WebSocketListener() {
                        override fun onMessage(webSocket: WebSocket, text: String) {
                            parseEvent(text)?.let { trySend(it) }
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            close(t)
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            close()
                        }
                    }

                val socket = okHttpClient.newWebSocket(request, listener)
                awaitClose { socket.close(1000, "client closed") }
            }

        private fun parseEvent(text: String): GitziWireEvent? {
            val envelope = json.parseToJsonElement(text).jsonObject
            return when (envelope["type"]?.jsonPrimitive?.content) {
                "epics" -> GitziWireEvent.Epics(json.decodeFromJsonElement(EpicListSerializer, envelope.getValue("epics")))
                "tasks" -> GitziWireEvent.Tasks(json.decodeFromJsonElement(TaskListSerializer, envelope.getValue("tasks")))
                "review_queue" ->
                    GitziWireEvent.ReviewQueue(
                        json.decodeFromJsonElement(ReviewItemListSerializer, envelope.getValue("items")),
                    )
                "chat" -> GitziWireEvent.Chat(json.decodeFromJsonElement(ChatListSerializer, envelope.getValue("messages")))
                "config" -> GitziWireEvent.Config(json.decodeFromJsonElement(ConfigDto.serializer(), envelope.getValue("config")))
                else -> null
            }
        }

        companion object {
            private val EpicListSerializer = kotlinx.serialization.builtins.ListSerializer(EpicDto.serializer())
            private val TaskListSerializer = kotlinx.serialization.builtins.ListSerializer(TaskDto.serializer())
            private val ReviewItemListSerializer = kotlinx.serialization.builtins.ListSerializer(ReviewItemDto.serializer())
            private val ChatListSerializer = kotlinx.serialization.builtins.ListSerializer(ChatMessageDto.serializer())
        }
    }
