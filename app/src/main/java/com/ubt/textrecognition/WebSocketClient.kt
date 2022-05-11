package com.ubt.textrecognition

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception

class WebSocketClient(
    private val onReceived: (r: TextOcrResult?) -> Unit
) {
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private var session: ClientWebSocketSession? = null

    fun start(success: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            kotlin.runCatching {
                Timber.d("启动websocket服务")
                client.webSocket(host = "192.168.3.13", port = 6789) {
                    session = this
                    Timber.d("连接成功")
                    withContext(Dispatchers.Main) {
                        success()
                    }
                    for(frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val receivedText = frame.readText()
//                                Timber.d("receivedText: $receivedText")
                                val r = try {
                                    Gson().fromJson<TextOcrResult>(receivedText, TextOcrResult::class.java)
                                } catch (e: Exception) {
                                    Timber.e(e)
                                    null
                                }
                                withContext(Dispatchers.Main) {
                                    onReceived(r)
                                }
                            }
                            is Frame.Binary -> {
                                Timber.d("接收到二进制消息")
                            }
                            is Frame.Close -> {
                                Timber.d("接收到Frame.Close消息")
                            }
                            else -> {

                            }
                        }
                    }

                }
            }.onFailure { e ->
                Timber.d("启动websocket服务失败")
                Timber.e(e)
            }
        }
    }

    suspend fun send(image: String?) {
        if (image == null) return
        val resTxt = JsonObject()
        resTxt.addProperty("model", "chinese_ocr")
        resTxt.addProperty("image", image)
        session?.send(resTxt.toString())
    }

    fun stop() {
        client.close()
    }
}