package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.VarItem
import dfk.template.DFTemplate
import parser.TreeNode

open class DFLObject {
    val fields: MutableMap<String, AstConverter> = mutableMapOf()
    val functions: MutableMap<String, AstConverter> = mutableMapOf()

    open fun accessField(name: String): AstConverter? {
        return fields[name]
    }

    open fun accessFunc(name: String): AstConverter? {
        return functions[name]
    }
}

class DFLPlayer : DFLObject {
    val target: String
    val textToDFFuncMap = mapOf(
        "send" to "SendMessage",
        "heal" to "Heal",
        "hurt" to "Damage",
        "damage" to "Damage"
    )

    class NameField(val player: DFLPlayer) : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            return VarItem.gameValue("Name ", player.target) // It's "Name " because "Name" is the string variant, "Name " returns a styled text
        }
    }

    class StrNameField(val player: DFLPlayer) : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            return VarItem.gameValue("Name", player.target) // It's "Name" because "Name " is the styled text variant, "Name" returns a string
        }
    }

    class PlayerActionFunc(val player: DFLPlayer, val name: String) : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            val contents = tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem }
            template.addCodeBlock(DFCodeBlock(DFCodeType.PLAYER_ACTION, name, player.target).setContent(*contents.toTypedArray()))
        }
    }

    override fun accessFunc(name: String): AstConverter? {
        if (functions.containsKey(name)) return functions[name]
        if (textToDFFuncMap.containsKey(name)) return PlayerActionFunc(this, textToDFFuncMap[name]!!)
        return null
    }

    constructor(target: String) {
        this.target = target

        fields["name"] = NameField(this)
        fields["string_name"] = StrNameField(this)
    }

}

object DefaultObject : DFLObject() {
    class StringFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val variable = VarItem.tempVar()
            val contents = mutableListOf(variable)
            contents.addAll(tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem })
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "String").setContent(*contents.toTypedArray()))
            return variable
        }
    }

    class StyledTextFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val variable = VarItem.tempVar()
            val contents = mutableListOf(variable)
            contents.addAll(tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem })
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "StyledText").setContent(*contents.toTypedArray()))
            return variable
        }
    }

    class NumberFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            if (tree.arguments.size == 0) return VarItem.num(0)
            val variable = VarItem.tempVar()
            var arg = TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem
            if (tree.arguments.size > 1) {
                arg = StringFunction().convert(tree, template, objects)
            }
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "ParseNumber").setContent(variable, arg))
            return variable
        }
    }

    override fun accessFunc(name: String): AstConverter? {
        functions["str"] = StringFunction()
        functions["string"] = StringFunction()
        functions["styled"] = StyledTextFunction()
        functions["num"] = NumberFunction()
        functions["number"] = NumberFunction()
        return super.accessFunc(name)
    }
}