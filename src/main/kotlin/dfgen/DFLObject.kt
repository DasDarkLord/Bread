package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.VarItem
import dfk.template.DFTemplate
import parser.TreeNode

open class DFLObject {
    val fields: MutableMap<String, AstConverter> = mutableMapOf()
    val functions: MutableMap<String, AstConverter> = mutableMapOf()

    fun accessField(name: String): AstConverter? {
        return fields[name]
    }

    fun accessFunc(name: String): AstConverter? {
        return functions[name]
    }
}

class DFLPlayer : DFLObject {
    val target: String

    class SendFunction(val player: DFLPlayer) : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            val contents = tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem }
            template.addCodeBlock(DFCodeBlock(DFCodeType.PLAYER_ACTION, "SendMessage", player.target).setContent(*contents.toTypedArray()))
        }
    }

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

    constructor(target: String) {
        this.target = target

        functions["send"] = SendFunction(this)

        fields["name"] = NameField(this)
        fields["string_name"] = StrNameField(this)
    }

}