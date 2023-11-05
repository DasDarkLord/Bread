package parser

import dfk.item.DFVarType

sealed class Ast {
    class Event(val name: String, val code: Block, val eventType: EventType) : Ast() {
        override fun toString(): String {
            return "{\"type\":\"event\",\"code\":$code,\"event\":$eventType"
        }
    }
    class Block(val nodes: List<Ast.Command>, val eventName: String) : Ast() {
        override fun toString(): String {
            return "{\"nodes\":$nodes,\"event\":$eventName}"
        }
    }
    class Command(val tree: TreeNode) : Ast() {
        override fun toString(): String {
            return tree.toString()
        }
    }
}

sealed class EventType {
    data object Event : EventType() {
        override fun toString(): String = "Event"
    }

    data class Function(val parameters: MutableMap<String, DFVarType>) : EventType() {
        override fun toString(): String = "Function"
    }

    data object Process : EventType() {
        override fun toString(): String = "Process"
    }
}