package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.template.DFTemplate
import parser.Ast
import parser.EventType

fun convertAstToDF(events: List<Ast.Event>): List<DFTemplate> {
    val eventNames = mapOf(
        "join" to "Join",
        "leave" to "Leave",
        "sneak" to "Sneak",
        "leftClick" to "LeftClick",
        "lc" to "LeftClick",
        "rightClick" to "RightClick",
        "rc" to "RightClick"
    )

    val templates = mutableListOf<DFTemplate>()
    for (event in events) {
        val template = DFTemplate()

        val type: DFCodeType = when (event.eventType) {
            EventType.Event -> DFCodeType.PLAYER_EVENT
            else -> throw IllegalStateException("Unsupported event type ${event.eventType}")
        }
        val code = event.code
        template.addCodeBlock(DFCodeBlock(type, eventNames[code.eventName] ?: "?"))

        val defaultObjects = mutableMapOf<String, DFLObject>(
            "player" to DFLPlayer("Default"),
            "default" to DFLPlayer("Default"),
            "selection" to DFLPlayer("Selection"),
            "killer" to DFLPlayer("killer"),
            "damager" to DFLPlayer("damager"),
            "victim" to DFLPlayer("victim"),
            "shooter" to DFLPlayer("shooter")
        )

        val nodes = code.nodes
        for (node in nodes) {
            TreeConverter.convertTree(node.tree, template, defaultObjects)
        }

        templates.add(template)
    }

    return templates
}