package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.DFVarType
import dfk.item.VarItem
import dfk.template.DFTemplate
import parser.Ast
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
        "send" to mapOf("name" to "SendMessage", "tags" to mapOf("Text Value Merging" to "No spaces")),
        "heal" to "Heal",
        "hurt" to "Damage",
        "damage" to "Damage",
        "teleport" to "Teleport",
        "tp" to "Teleport"
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

    class LocationField(val player: DFLPlayer) : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
            return VarItem.gameValue("Location", player.target)
        }
    }

    class PlayerActionFunc(val player: DFLPlayer, val action: Any) : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            val name: String
            if (action is String) name = action
            else name = (action as Map<*, *>)["name"]!! as String

            val contents = tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem }
            var codeBlock = DFCodeBlock(DFCodeType.PLAYER_ACTION, name, player.target).setContent(*contents.toTypedArray())
            if (action is Map<*, *>) {
                if (action["tags"] != null) {
                    for (entry in action["tags"] as Map<*, *>) {
                        codeBlock = codeBlock.setTag(entry.key as String, entry.value as String)
                    }
                }
                if (action["invert"] != null) {
                    codeBlock.inverter = action["invert"] as String
                }
            }

            template.addCodeBlock(codeBlock)
        }
    }

    override fun accessFunc(name: String): AstConverter? {
        if (functions.containsKey(name)) return functions[name]
        if (textToDFFuncMap.containsKey(name)) {
            return PlayerActionFunc(this, textToDFFuncMap[name]!!)
        }
        return null
    }

    constructor(target: String) {
        this.target = target

        fields["name"] = NameField(this)
        fields["string_name"] = StrNameField(this)
        fields["location"] = LocationField(this)

        fields["sprinting"] = CustomPlayerIf("IsSprinting")
        fields["sneaking"] = CustomPlayerIf("IsSneaking")
        fields["grounded"] = CustomPlayerIf("IsGrounded")
        fields["swimming"] = CustomPlayerIf("IsSwimming")
        fields["blocking"] = CustomPlayerIf("IsBlocking")
        fields["usingPack"] = CustomPlayerIf("UsingPack")
        fields["gliding"] = CustomPlayerIf("IsGliding")
        fields["flying"] = CustomPlayerIf("IsFlying")
    }

    class CustomPlayerIf(val name: String) : CustomIf {
        override fun customIf(block: Ast.Block, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            template.addCodeBlock(DFCodeBlock(DFCodeType.IF_PLAYER, name))
            template.addCodeBlock(DFCodeBlock.bracket(true))
            for (node in block.nodes) TreeConverter.convertTree(node.tree, template, objects)
            template.addCodeBlock(DFCodeBlock.bracket(false))
        }

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

    class LocationFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            if (tree.arguments.size < 3) return VarItem.loc(0.0, 0.0, 0.0)
            val x = (TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem).value as Double
            val y = (TreeConverter.convertTree(tree.arguments[1], template, objects) as VarItem).value as Double
            val z = (TreeConverter.convertTree(tree.arguments[2], template, objects) as VarItem).value as Double
            if (tree.arguments.size == 5) {
                val pitch = ((TreeConverter.convertTree(tree.arguments[3], template, objects) as VarItem).value as Double).toFloat()
                val yaw = ((TreeConverter.convertTree(tree.arguments[4], template, objects) as VarItem).value as Double).toFloat()
                return VarItem.loc(x, y, z, pitch, yaw)
            }
            return VarItem.loc(x, y, z)
        }
    }

    class WaitFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            var duration: VarItem = VarItem.num(0)
            var unit = "Ticks"
            if (tree.arguments.size > 0) {
                val arg1 = TreeConverter.convertTree(tree.arguments[0], template, objects) as? VarItem
                if (arg1 != null) {
                    if (arg1.type == DFVarType.NUMBER) duration = arg1
                    if (arg1.type == DFVarType.STRING) unit = arg1.value as String
                }

                if (tree.arguments.size > 1) {
                    val arg2 = TreeConverter.convertTree(tree.arguments[0], template, objects) as? VarItem
                    if (arg2 != null) {
                        if (arg2.type == DFVarType.NUMBER) duration = arg2
                        if (arg2.type == DFVarType.STRING) unit = arg2.value as String
                    }
                }
            }

            template.addCodeBlock(DFCodeBlock(DFCodeType.CONTROL, "Wait").setContent(duration).setTag("Time Unit", unit))
        }
    }

    override fun accessFunc(name: String): AstConverter? {
        functions["str"] = StringFunction()
        functions["string"] = StringFunction()
        functions["styled"] = StyledTextFunction()
        functions["num"] = NumberFunction()
        functions["number"] = NumberFunction()
        functions["loc"] = LocationFunction()
        functions["location"] = LocationFunction()

        functions["wait"] = WaitFunction()
        return super.accessFunc(name)
    }
}

interface CustomIf : AstConverter {
    fun customIf(block: Ast.Block, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any?

    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
        return customIf(tree.left!!.value!! as Ast.Block, tree, template, objects)
    }
}

object WordObject : DFLObject() {

    class LocShiftFunction(val axis: String) : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            val arg0 = TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem

            if (axis == "all") {
                val arg1 = TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem
                val arg2 = TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem
                template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "ShiftAllAxes").setContent(v, arg0, arg1, arg2))
            } else {
                template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "ShiftOnAxis").setContent(v, arg0).setTag("Coordinate", axis))
            }
        }
    }

    class RepeatStringFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val repeatTimes = TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem
            if (v.type == DFVarType.VARIABLE) {
                template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "RepeatString").setContent(v, v, repeatTimes))
                return v
            } else {
                val variable = VarItem.tempVar()
                template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "RepeatString").setContent(variable, v, repeatTimes))
                return variable
            }
        }

    }

    override fun accessFunc(name: String): AstConverter? {
        functions["shift"]  = LocShiftFunction("all")
        functions["shiftX"] = LocShiftFunction("X")
        functions["shiftY"] = LocShiftFunction("Y")
        functions["shiftZ"] = LocShiftFunction("Z")
        functions["repeat"] = RepeatStringFunction()
        return super.accessFunc(name)
    }

    interface WordObjectConverter : AstConverter {
        fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any?

        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
            val args = tree.arguments.toMutableList()
            args.removeAt(0)
            val newTree = tree.copy(arguments = args)

            return convert(TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem, newTree, template, objects)
        }
    }

}