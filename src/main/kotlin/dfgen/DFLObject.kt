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
        "tp" to "Teleport",
        "actionbar" to "ActionBar"
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
        override fun customIf(block: Ast.Block, inverted: Boolean, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
            val cb = DFCodeBlock(DFCodeType.IF_PLAYER, name)
            cb.inverter = if (inverted) "NOT" else ""
            template.addCodeBlock(cb)
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
            VariableTracker.setSavedType(variable, DFVarType.STRING)
            return variable
        }
    }

    class StyledTextFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val variable = VarItem.tempVar()
            val contents = mutableListOf(variable)
            contents.addAll(tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem })
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "StyledText").setContent(*contents.toTypedArray()))
            VariableTracker.setSavedType(variable, DFVarType.STYLED_TEXT)
            return variable
        }
    }

    class ParseStyledTextFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val variable = VarItem.tempVar()
            val contents = mutableListOf(variable)
            contents.addAll(tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem })
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "ParseMiniMessageExpr").setContent(*contents.toTypedArray()))
            VariableTracker.setSavedType(variable, DFVarType.STYLED_TEXT)
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
            VariableTracker.setSavedType(variable, DFVarType.NUMBER)
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
            var duration: VarItem = VarItem.num(1)
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

    class ListLengthFunction : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val list = TreeConverter.convertTree(tree.arguments[0], template, objects)
            if (list !is VarItem) return VarItem.num(0)

            val variable = VarItem.tempVar()
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "ListLength").setContent(variable, list))
            VariableTracker.setSavedType(variable, DFVarType.NUMBER)
            return variable
        }
    }

    class CreateDictFunction() : AstConverter {
        override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val keyList = TreeConverter.convertTree(tree.arguments[0], template, objects)
            if (keyList !is VarItem) return VarItem.num(0)
            val valList = TreeConverter.convertTree(tree.arguments[1], template, objects)
            if (valList !is VarItem) return VarItem.num(0)

            val variable = VarItem.tempVar()
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "CreateDict").setContent(variable, keyList, valList))
            return variable
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
        functions["parsestyled"] = ParseStyledTextFunction()
        functions["parseStyled"] = ParseStyledTextFunction()
        functions["listlen"] = ListLengthFunction()
        functions["listLen"] = ListLengthFunction()
        functions["listLength"] = ListLengthFunction()
        functions["listlength"] = ListLengthFunction()
        functions["createdict"] = CreateDictFunction()
        functions["createDict"] = CreateDictFunction()

        val mathFunctions = buildMap {
            put("sin", DFCodeBlock(DFCodeType.SET_VARIABLE, "Sine").setTag("Input", "Degrees"))
            put("sinrad", DFCodeBlock(DFCodeType.SET_VARIABLE, "Sine").setTag("Input", "Radians"))
            put("cos", DFCodeBlock(DFCodeType.SET_VARIABLE, "Cosine").setTag("Input", "Degrees"))
            put("cosrad", DFCodeBlock(DFCodeType.SET_VARIABLE, "Cosine").setTag("Input", "Radians"))
            put("tan", DFCodeBlock(DFCodeType.SET_VARIABLE, "Tangent").setTag("Input", "Degrees"))
            put("tanrad", DFCodeBlock(DFCodeType.SET_VARIABLE, "Tangent").setTag("Input", "Radians"))
            put("clamp", DFCodeBlock(DFCodeType.SET_VARIABLE, "Clamp"))
            put("wrap", DFCodeBlock(DFCodeType.SET_VARIABLE, "Wrap"))
            put("abs", DFCodeBlock(DFCodeType.SET_VARIABLE, "Absolute"))
            put("log", DFCodeBlock(DFCodeType.SET_VARIABLE, "Logarithm"))
            put("round", DFCodeBlock(DFCodeType.SET_VARIABLE, "RoundNumber"))
            put("floor", DFCodeBlock(DFCodeType.SET_VARIABLE, "RoundNumber").setTag("Round Mode", "Floor"))
            put("ceil", DFCodeBlock(DFCodeType.SET_VARIABLE, "RoundNumber").setTag("Round Mode", "Ceiling"))
            put("root", DFCodeBlock(DFCodeType.SET_VARIABLE, "Root"))
            put("min", DFCodeBlock(DFCodeType.SET_VARIABLE, "MinNumber"))
            put("max", DFCodeBlock(DFCodeType.SET_VARIABLE, "MaxNumber"))
            put("random", DFCodeBlock(DFCodeType.SET_VARIABLE, "RandomNumber"))
        }
        for (mathFunc in mathFunctions) {
            val converter = object : AstConverter {
                override fun convert(
                    tree: TreeNode,
                    template: DFTemplate,
                    objects: MutableMap<String, DFLObject>,
                ): Any {
                    val variable = VarItem.tempVar()
                    mathFunc.value.setContent(0, variable)
                    var index = 1
                    for (arg in tree.arguments) {
                        val conv = TreeConverter.convertTree(arg, template, objects)
                        if (conv !is VarItem) continue

                        mathFunc.value.setContent(index, conv)
                        index++
                    }
                    return variable
                }
            }
            functions[mathFunc.key] = converter
        }

        functions["wait"] = WaitFunction()
        return super.accessFunc(name)
    }
}

interface CustomIf : AstConverter {
    fun customIf(block: Ast.Block, inverted: Boolean, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any?

    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
        val inverted = if (tree.arguments.isNotEmpty()) if (tree.arguments[0].value is Boolean) tree.arguments[0].value as Boolean else false else false
        return customIf(tree.left!!.value!! as Ast.Block, inverted, tree, template, objects)
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
            VariableTracker.setSavedType(v, DFVarType.LOCATION)
        }
    }

    class RepeatStringFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            println("arg 0 " + tree.arguments[0])
            val repeatTimes = TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem
            if (v.type == DFVarType.VARIABLE) {
                template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "RepeatString").setContent(v, v, repeatTimes))
                VariableTracker.setSavedType(v, DFVarType.STRING)
                return v
            } else {
                val variable = VarItem.tempVar()
                template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "RepeatString").setContent(variable, v, repeatTimes))
                VariableTracker.setSavedType(variable, DFVarType.STRING)
                return variable
            }
        }

    }

    class StringSplitFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val variable = VarItem.tempVar()
            val splitter: String = if (tree.arguments.isNotEmpty()) {
                val s = TreeConverter.convertTree(tree.arguments[0], template, objects)
                if (s is VarItem && s.type == DFVarType.STRING) {
                    s.value as String
                } else " "
            } else " "
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "SplitString").setContent(variable, v, VarItem.str(splitter)))
            VariableTracker.setSavedType(variable, DFVarType.LIST)
            return variable
        }
    }

    class ListAddFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            var cb = DFCodeBlock(DFCodeType.SET_VARIABLE, "AppendValue").setContent(v)
            for ((index, argument) in tree.arguments.withIndex()) {
                cb = cb.setContent(index + 1, TreeConverter.convertTree(argument, template, objects) as VarItem)
            }
            template.addCodeBlock(cb)
            return v
        }
    }

    class ListInsertFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            var cb = DFCodeBlock(DFCodeType.SET_VARIABLE, "InsertListValue").setContent(v)
            for ((index, argument) in tree.arguments.withIndex()) {
                cb = cb.setContent(index + 1, TreeConverter.convertTree(argument, template, objects) as VarItem)
            }
            template.addCodeBlock(cb)
            return v
        }
    }

    class ListRemoveFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            var cb = DFCodeBlock(DFCodeType.SET_VARIABLE, "RemoveListIndex").setContent(v)
            for ((index, argument) in tree.arguments.withIndex()) {
                cb = cb.setContent(index + 1, TreeConverter.convertTree(argument, template, objects) as VarItem)
            }
            template.addCodeBlock(cb)
            return v
        }
    }

    class ListJoinFunction() : WordObjectConverter {
        override fun convert(v: VarItem, tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
            val variable = VarItem.tempVar()
            var cb = DFCodeBlock(DFCodeType.SET_VARIABLE, "JoinString").setContent(variable, v)
            if (tree.arguments.isNotEmpty()) {
                cb.setContent(2, TreeConverter.convertTree(tree.arguments[0], template, objects) as VarItem)
                if (tree.arguments.size > 1) {
                    cb.setContent(3, TreeConverter.convertTree(tree.arguments[1], template, objects) as VarItem)
                }
            }
            template.addCodeBlock(cb)
            VariableTracker.setSavedType(variable, DFVarType.STRING)
            return variable
        }

    }

    override fun accessFunc(name: String): AstConverter? {
        functions["shift"]  = LocShiftFunction("all")
        functions["shiftX"] = LocShiftFunction("X")
        functions["shiftY"] = LocShiftFunction("Y")
        functions["shiftZ"] = LocShiftFunction("Z")
        functions["repeat"] = RepeatStringFunction()
        functions["split"] = StringSplitFunction()
        functions["add"] = ListAddFunction()
        functions["insert"] = ListInsertFunction()
        functions["removeat"] = ListRemoveFunction()
        functions["removeAt"] = ListRemoveFunction()
        functions["join"] = ListJoinFunction()
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

class EventObject : DFLObject() {

    fun addField(name: String, value: VarItem): EventObject {
        fields[name] = object : AstConverter {
            override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
                return value
            }
        }
        return this
    }

}