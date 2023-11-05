package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.VarItem
import dfk.template.DFTemplate
import parser.Ast
import parser.EventType

fun convertAstToDF(events: List<Ast.Event>): List<DFTemplate> {
    val eventNames = mapOf(
        "join" to "Join",
        "leave" to "Leave",
        "sneak" to "Sneak",
        "leftclick" to "LeftClick",
        "lc" to "LeftClick",
        "rightclick" to "RightClick",
        "rc" to "RightClick"
    )

    val templates = mutableListOf<DFTemplate>()
    for (event in events) {
        val template = DFTemplate()

        val type: DFCodeType
        if (event.eventType == EventType.Event) type = DFCodeType.PLAYER_EVENT
        else if (event.eventType is EventType.Function) type = DFCodeType.FUNCTION
        else if (event.eventType == EventType.Process) type = DFCodeType.PROCESS
        else throw UnsupportedOperationException("Unsupported event type ${event.eventType}")

        val code = event.code
        val name = if (event.eventType is EventType.Function || event.eventType is EventType.Process) code.eventName else eventNames[code.eventName] ?: code.eventName
        var cb = DFCodeBlock(type, name)
        if (event.eventType is EventType.Function) {
            var index = 0
            for (entry in event.eventType.parameters) {
                cb = cb.setContent(index, VarItem.parameter(entry.key, entry.value))
                index++
            }
        }
        template.addCodeBlock(cb)

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