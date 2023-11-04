package dfk.template

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class CodeClient {

    companion object {
        fun sendTemplate(templates: List<DFTemplate>) {
            val client = WSCCClient(URI("ws://localhost:31375"), templates.map { it.compressed() })
            client.connectionLostTimeout = 5000
            client.connect()
        }
    }

    class WSCCClient(uri: URI, private val templates: List<String>) : WebSocketClient(uri) {

        override fun onMessage(p0: String?) {
            if (p0 == "auth") {
                send("clear")
                send("place")
                for (template in templates) send("place $template")
                send("place go")
            }
            if (p0 == "place done") close()
        }

        override fun onOpen(p0: ServerHandshake?) { }

        override fun onClose(p0: Int, p1: String?, p2: Boolean) { }

        override fun onError(p0: Exception?) { }
    }

}