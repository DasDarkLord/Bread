package parser

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dfk.item.DFVarType

sealed class Ast {
    class Event(val name: String, val code: Block, val eventType: EventType) : Ast() {
        override fun toString(): String {
            return json().toString()
        }

        fun json(): JsonObject {
            val json = JsonObject()
            json.addProperty("type", "event")
            json.add("code", code.json())
            json.add("event", eventType.json())
            return json
        }
    }
    class Block(val nodes: List<Command>, val eventName: String) : Ast() {
        override fun toString(): String {
            return json().toString()
        }

        fun json(): JsonObject {
            val nodes = JsonArray()
            for (node in this.nodes) {
                nodes.add(node.json())
            }

            val json = JsonObject()
            json.add("nodes", nodes)
            json.addProperty("event", eventName)
            return json
        }
    }
    class Command(val tree: TreeNode) : Ast() {
        override fun toString(): String {
            return tree.toString()
        }

        fun json(): JsonObject {
            return tree.json()
        }
    }
}

sealed class EventType {
    data object Event : EventType() {
        override fun toString(): String {
            return json().toString()
        }

        override fun json(): JsonObject {
            val json = JsonObject()
            json.addProperty("type", "event")
            return json
        }
    }

    data class Function(val parameters: MutableMap<String, DFVarType>) : EventType() {
        override fun toString(): String {
            var param = ""
            var i = 0
            for ((k, v) in parameters) {
                param += "{\"$k\":\"${v.name}\"}" + if (i + 1 < parameters.size) "," else ""
                i++
            }
            return "{\"type\":\"function\",\"parameters\":[$param]}"
        }

        override fun json(): JsonObject {
            return Gson().fromJson(toString(), JsonObject::class.java)
        }
    }

    data object Process : EventType() {
        override fun toString(): String {
            return json().toString()
        }

        override fun json(): JsonObject {
            val json = JsonObject()
            json.addProperty("type", "proc")
            return json
        }
    }

    open fun json(): JsonObject {
        return JsonObject()
    }
}