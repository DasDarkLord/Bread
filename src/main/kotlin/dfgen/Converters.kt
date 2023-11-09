package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.DFVarType
import dfk.item.DFVariable
import dfk.item.VarItem
import dfk.template.DFTemplate
import pairs
import parser.Ast
import parser.TreeNode

val astConverters = mapOf(
    "number" to NumberConverter, "word" to WordConverter, "string" to StringConverter, "styled" to StyledTextConverter, "list" to ListConverter, "dict" to DictConverter,
    "true" to TrueConverter, "false" to FalseConverter,
    "assign" to AssignmentConverter, "acc" to AccessorConverter,
    "add" to AdditionConverter, "sub" to SubtractionConverter, "mul" to MultiplicationConverter, "div" to DivisionConverter, "pow" to ExponentConverter, "rem" to RemainderConverter, "mod" to ModuloConverter,
    "inc" to IncrementConverter, "dec" to DecrementConverter,
    "call" to CallConverter, "start_proc" to StartProcConverter,
    "cond" to IfConverter, "repeat" to RepeatConverter,
    "index" to IndexConverter,
    "stop" to StopConverter, "ret" to ReturnConverter,
    "is" to IsConverter
)

interface AstConverter {
    fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any?
}

object NumberConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        return VarItem.num(tree.value.toString())
    }
}

object StringConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        return VarItem.str(tree.value.toString())
    }
}

object StyledTextConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        return VarItem.styled(tree.value.toString())
    }
}

object TrueConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        return VarItem.num(1)
    }
}

object FalseConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        return VarItem.num(0)
    }
}

object ListConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val variable = VarItem.tempVar()
        val groups = mutableListOf<MutableList<VarItem>>()
        val currentGroup = mutableListOf<VarItem>()
        for (value in tree.value!! as List<*>) {
            if (value !is TreeNode) continue
            val b = TreeConverter.convertTree(value, template, objects)
            if (b !is VarItem) continue
            currentGroup.add(b)
            if (currentGroup.size >= 26) {
                groups.add(currentGroup.toMutableList())
                currentGroup.clear()
            }
        }
        if (currentGroup.isNotEmpty()) groups.add(currentGroup)

        val listRecreation = mutableListOf<VarItem>()
        for ((index, group) in groups.withIndex()) {
            val action = if (index == 0) "CreateList" else "AppendList"
            val items = group.toMutableList()
            items.add(0, variable)
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, action).setContent(*items.toTypedArray()))

            listRecreation.addAll(items)
        }

        VariableTracker.setSavedType(variable, VarItem(DFVarType.LIST, listRecreation))

        return variable
    }
}

object DictConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val variable = VarItem.tempVar()
        val keyVariable = VarItem.tempVar()
        val valueVariable = VarItem.tempVar()
        val keyGroups = mutableListOf<MutableList<VarItem>>()
        val valueGroups = mutableListOf<MutableList<VarItem>>()
        val currentKeyGroup = mutableListOf<VarItem>()
        val currentValueGroup = mutableListOf<VarItem>()
        for (value in tree.value!! as Map<*, *>) {
            if (value.key !is TreeNode) continue
            if (value.value !is TreeNode) continue
            val b = TreeConverter.convertTree(value.key as TreeNode, template, objects)
            if (b !is VarItem) continue
            currentKeyGroup.add(b)
            if (currentKeyGroup.size >= 26) {
                keyGroups.add(currentKeyGroup.toMutableList())
                currentKeyGroup.clear()
            }

            val c = TreeConverter.convertTree(value.value as TreeNode, template, objects)
            if (c !is VarItem) continue
            currentValueGroup.add(c)
            if (currentKeyGroup.size >= 26) {
                valueGroups.add(currentValueGroup.toMutableList())
                currentValueGroup.clear()
            }
        }
        if (currentKeyGroup.isNotEmpty()) {
            keyGroups.add(currentKeyGroup)
            valueGroups.add(currentValueGroup)
        }

        val mapRecreation = mutableMapOf<VarItem, VarItem>()
        for ((index, group) in keyGroups.withIndex()) {
            val action = if (index == 0) "CreateList" else "AppendList"
            val items = group.toMutableList()
            items.add(0, keyVariable)
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, action).setContent(*items.toTypedArray()))

            val valAction = if (index == 0) "CreateList" else "AppendList"
            val valItems = valueGroups[index].toMutableList()
            valItems.add(0, valueVariable)
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, valAction).setContent(*valItems.toTypedArray()))

            for ((itemIndex, item) in items.withIndex()) {
                val value = valItems[itemIndex]
                mapRecreation[item] = value
            }
        }

        VariableTracker.setSavedType(keyVariable, DFVarType.LIST)
        VariableTracker.setSavedType(valueVariable, DFVarType.LIST)

        template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "CreateDict").setContent(variable, keyVariable, valueVariable))
        VariableTracker.setSavedType(variable, VarItem(DFVarType.DICT, mapRecreation))

        return variable
    }
}

object IndexConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val v = TreeConverter.convertTree(tree.left!!, template, objects)
        if (v !is VarItem) return VarItem.num(0)
        val left = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        val variable = VarItem.tempVar()
        println("$v is type of ${v.type} - ${VariableTracker.getSavedItem(  v)}")
        if (VariableTracker.getSavedType(v) == DFVarType.LIST) {
            val plusOne = VarItem.num("%math(1+${left})")
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "GetListValue").setContent(variable, v, plusOne))
            println(variable)
            println(v)
            println(left)
            println("list")
            println(VariableTracker.getSavedItem(v))
            println(VariableTracker.getSavedItem(v).value)
            try {
                VariableTracker.setSavedType(variable, (VariableTracker.getSavedItem(v).value as List<*>)[left.value.toString().toIntOrNull() ?: 0] as? VarItem ?: VarItem.num(0))
            } catch (ignored: Exception) { }
        } else if (VariableTracker.getSavedType(v) == DFVarType.DICT) {
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "GetDictValue").setContent(variable, v, left))
            println(variable)
            println(v)
            println(left)
            println("dict")
            println(VariableTracker.getSavedItem(v))
            println(VariableTracker.getSavedItem(v).value)
            try {
                VariableTracker.setSavedType(variable, (VariableTracker.getSavedItem(v).value as Map<*, *>)[left.value.toString()] as? VarItem ?: VarItem.num(0))
            } catch (ignored: Exception) { }
        }

        return variable
    }
}

object AdditionConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        var left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        var right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        val leftType = VariableTracker.getSavedType(left)
        val rightType = VariableTracker.getSavedType(right)
        val rTypes = listOf(DFVarType.STYLED_TEXT, DFVarType.STRING, DFVarType.VARIABLE, DFVarType.NUMBER)
        if (leftType !in rTypes) {
            val tempVar = VarItem.tempVar()
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "=").setContent(tempVar, left))
            left = tempVar
        }
        if (rightType !in rTypes) {
            val tempVar = VarItem.tempVar()
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "=").setContent(tempVar, right))
            right = tempVar
        }
        if (leftType == DFVarType.STYLED_TEXT || rightType == DFVarType.STYLED_TEXT) {
            return VarItem.styled("${left}${right}")
        }
        if (leftType == DFVarType.STRING || rightType == DFVarType.STRING) {
            return VarItem.str("${left}${right}")
        }
        return VarItem.num("%math(${left}+${right})")
    }
}

object SubtractionConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        return VarItem.num("%math(${left}-${right})")
    }
}

object MultiplicationConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        return VarItem.num("%math(${left}*${right})")
    }
}

object DivisionConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        return VarItem.num("%math(${left}/${right})")
    }
}

object ExponentConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        val variable = VarItem.tempVar()
        template.addCodeBlock(DFCodeBlock(
            DFCodeType.SET_VARIABLE,
            "Exponent"
        ).setContent(variable, left, right))
        VariableTracker.setSavedType(variable, DFVarType.NUMBER)
        return variable
    }
}

object RemainderConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        val variable = VarItem.tempVar()
        template.addCodeBlock(DFCodeBlock(
            DFCodeType.SET_VARIABLE,
            "%"
        ).setContent(variable, left, right))
        VariableTracker.setSavedType(variable, DFVarType.NUMBER)
        return variable
    }
}

object ModuloConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        val variable = VarItem.tempVar()
        template.addCodeBlock(DFCodeBlock(
            DFCodeType.SET_VARIABLE,
            "%"
        ).setContent(variable, left, right).setTag("Remainder Mode", "Modulo"))
        VariableTracker.setSavedType(variable, DFVarType.NUMBER)
        return variable
    }
}

object IncrementConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val variable = TreeConverter.convertTree(tree.value!! as TreeNode, template, objects) as VarItem
        template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "+=").setContent(variable)
        )
        VariableTracker.setSavedType(variable, DFVarType.NUMBER)
        return variable
    }
}

object DecrementConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val variable = VarItem.tempVar()
        template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "-=")
            .setContent(variable, TreeConverter.convertTree(tree.value!! as TreeNode, template, objects) as VarItem)
        )
        VariableTracker.setSavedType(variable, DFVarType.NUMBER)
        return variable
    }
}

object AssignmentConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = WordConverter.convert(tree.left!!, template, objects)
        var rightNode = tree.right!!
        if (rightNode.type == "call") {
            println(rightNode)
            if (DefaultObject.accessFunc(rightNode.value!! as String) == null) {
                val args = rightNode.arguments.toMutableList()
                args.add(0, tree.left)
                rightNode = rightNode.copy(arguments = args)
                CallConverter.convert(rightNode, template, objects)
                return left
            }
        }
        val right = TreeConverter.convertTree(rightNode, template, objects)
        if (right is TreeNode || right is CustomIf) {
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "=").setContent(WordConverter.convert(tree.left, template, objects), FalseConverter.convert(tree.left, template, objects)))
            if ((right is TreeNode && (right.type == "eq" || right.type == "neq" || right.type == "geq" || right.type == "leq" ||
                        right.type == "lt" || right.type == "gt")) ||
                (right is CustomIf)) {
                val assignNode = TreeNode(
                    "assign",
                    left = tree.left,
                    right = TreeNode(
                        "number",
                        value = 1.0
                    )
                )

                var ifValue = right
                if (ifValue is CustomIf) ifValue = tree.right
                val ifNode = TreeNode(
                    "cond",
                    value = ifValue,
                    left = TreeNode(
                        "block",
                        value = Ast.Block(listOf(Ast.Command(assignNode)), "Block_")
                    )
                )
                IfConverter.convert(ifNode, template, objects)

                val item = TreeConverter.convertTree(tree.left, template, objects) as VarItem
                VariableTracker.setSavedType(item, DFVarType.NUMBER)
                return item
            }
            else throw UnsupportedOperationException("Expected a value or condition but got $right")
        } else {
            val rightVarItem = right as? VarItem ?: VarItem.num(0)
            template.addCodeBlock(
                DFCodeBlock(DFCodeType.SET_VARIABLE, "=")
                    .setContent(left, rightVarItem)
            )
            VariableTracker.setSavedType(left, VariableTracker.getSavedItem(rightVarItem))
            return rightVarItem
        }
    }
}

object WordConverter : AstConverter {
    val wordScopes = mutableMapOf<String, DFVariable.VariableScope>()
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        var scope = DFVariable.VariableScope.LINE
        if (wordScopes.containsKey(tree.value!! as String)) scope = wordScopes[tree.value]!!
        if (tree.arguments.isNotEmpty()) {
            scope = tree.arguments[0].value!! as DFVariable.VariableScope
        }
        wordScopes[tree.value as String] = scope
        return VarItem.variable(tree.value, scope)
    }
}

object AccessorConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
        val right = tree.right!!
        if (tree.left!!.type == "word") {
            val name = tree.left.value!! as String

            println(objects)
            println(name)

            if (!objects.containsKey(name)) {
                val newArgs = right.arguments.toMutableList()
                newArgs.add(0, tree.left)
                val rightWithArgs = right.copy(arguments = newArgs)

                return if (right.type == "call") {
                    val func = WordObject.accessFunc(right.value!! as String)!!
                    if (func is CustomIf) return func
                    func.convert(rightWithArgs, template, objects)
                } else {
                    val field = WordObject.accessField(right.value!! as String)!!
                    if (field is CustomIf) return field
                    field.convert(rightWithArgs, template, objects)
                }
            }

            val obj = objects[name]!!

            return if (right.type == "call") {
                val func = obj.accessFunc(right.value!! as String)!!
                if (func is CustomIf) return func
                func.convert(right, template, objects)
            } else {
                val field = obj.accessField(right.value!! as String)!!
                if (field is CustomIf) return field
                field.convert(right, template, objects)
            }
        } else {
            val newArgs = right.arguments.toMutableList()
            newArgs.add(0, tree.left)
            val rightWithArgs = right.copy(arguments = newArgs)

            return if (right.type == "call") {
                val func = WordObject.accessFunc(right.value!! as String)!!
                if (func is CustomIf) return func
                func.convert(rightWithArgs, template, objects)
            } else {
                val field = WordObject.accessField(right.value!! as String)!!
                if (field is CustomIf) return field
                field.convert(rightWithArgs, template, objects)
            }
        }
    }
}

object CallConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
        if (DefaultObject.accessFunc(tree.value!! as String) != null) return DefaultObject.accessFunc(tree.value as String)!!.convert(tree, template, objects)
        else {
            val varItemArgs = tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem }
            template.addCodeBlock(DFCodeBlock(DFCodeType.CALL_FUNCTION, tree.value as String)
                .setContent(*varItemArgs.toTypedArray()))
            for (function in pairs) {
                val functionName = function.first
                if (functionName != tree.value) continue

                val parameters = function.second
                println("Parameters $parameters\nArguments $varItemArgs")
                for ((index, arg) in varItemArgs.withIndex()) {
                    if (index >= parameters.keys.size) break
                    val parameter = parameters[parameters.keys.toTypedArray()[index]] ?: DFVarType.ANY
                    if (parameter == DFVarType.VARIABLE) {
                        VariableTracker.setSavedType(arg, VariableTracker.getSavedType(parameters.keys.toTypedArray()[index]))
                    }
                }
            }
        }
        return null
    }
}

object StartProcConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        template.addCodeBlock(DFCodeBlock(DFCodeType.START_PROCESS, tree.value as String)
            .setTag("Local Variables", tree.arguments[0].value as String)
            .setTag("Target Mode", tree.arguments[1].value as String))
    }
}

object IfConverter : AstConverter {
    private fun placeElse(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        if (tree.right == null) return
        val code = tree.right.value!! as Ast.Block

        template.addCodeBlock(DFCodeBlock(DFCodeType.ELSE))
        template.addCodeBlock(DFCodeBlock.bracket(true))
        for (command in code.nodes) TreeConverter.convertTree(command.tree, template, objects)
        template.addCodeBlock(DFCodeBlock.bracket(false))
    }

    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        val code = tree.left!!.value!! as Ast.Block
        val check = tree.value!! as TreeNode
        val inverted = if (tree.arguments.isNotEmpty()) if (tree.arguments[0].value is Boolean) tree.arguments[0].value as Boolean else false else false
        if (inverted) println(tree)

        val cbs = template.codeBlocks.size
        try {
            val v = TreeConverter.convertTree(check, template, objects)
            if (v is CustomIf) {
                v.customIf(code, inverted, tree, template, objects)
                placeElse(tree, template, objects)
                return
            } else throw java.lang.Exception()
        } catch (ignored: Exception) {
            var added = template.codeBlocks.size - cbs
            while (added > 0) {
                template.codeBlocks.removeAt(template.codeBlocks.size)
                added--
            }
        }

        var action = when (check.type) {
            "eq" -> "="
            "neq" -> "!="
            "lt" -> "<"
            "gt" -> ">"
            "geq" -> ">="
            "leq" -> "<="
            "match" -> "StringMatches"
            "in" -> "ListContains"
            else -> "?"
        }
        var ifCodeBlock = DFCodeBlock(DFCodeType.IF_VARIABLE, action)
        if (action == "?") { // Assume it's  `if (boolVariable)` syntax
            ifCodeBlock = ifCodeBlock.setContent(TreeConverter.convertTree(check, template, objects) as VarItem, TrueConverter.convert(tree, template, objects))
            ifCodeBlock.action = "="
        } else if (action == "ListContains") {
            val rightItem = TreeConverter.convertTree(check.right!!, template, objects) as VarItem
            if (VariableTracker.getSavedType(rightItem) == DFVarType.DICT) {
                action = "DictHasKey"
                ifCodeBlock.action = action
            }
            ifCodeBlock = ifCodeBlock.setContent(TreeConverter.convertTree(check.right, template, objects) as VarItem, TreeConverter.convertTree(check.left!!, template, objects) as VarItem)
        } else if (action == "StringMatches") {
            var rightNode = check.right!!
            if (rightNode.type == "regex") {
                rightNode = rightNode.left!!
                ifCodeBlock = ifCodeBlock.setTag("Regular Expressions", "Enable")
            }
            ifCodeBlock = ifCodeBlock.setContent(TreeConverter.convertTree(rightNode, template, objects) as VarItem, TreeConverter.convertTree(check.left!!, template, objects) as VarItem)
        } else {
            val items = mutableListOf(TreeConverter.convertTree(check.left!!, template, objects) as VarItem)
            if (action == "=" || action == "!=") {
                if (check.right!!.type == "list") {
                    for (value in (check.right.value as List<*>).map { TreeConverter.convertTree(it as TreeNode, template, objects) }) {
                        if (value is VarItem) items.add(value)
                    }
                } else {
                    items.add(TreeConverter.convertTree(check.right, template, objects) as VarItem)
                }
            } else {
                items.add(TreeConverter.convertTree(check.right!!, template, objects) as VarItem)
            }

            ifCodeBlock = ifCodeBlock.setContent(*items.toTypedArray())
        }
        if (inverted) ifCodeBlock.inverter = "NOT"

        template.addCodeBlock(ifCodeBlock)
        template.addCodeBlock(DFCodeBlock.bracket(true))
        for (command in code.nodes) TreeConverter.convertTree(command.tree, template, objects)
        template.addCodeBlock(DFCodeBlock.bracket(false))
        placeElse(tree, template, objects)
    }
}

object RepeatConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        val t = mapOf(
            "forever" to "Forever"
        )

        val code = tree.left!!.value!! as Ast.Block
        val type = t[tree.value!! as String] ?: tree.value as String

        template.addCodeBlock(DFCodeBlock(DFCodeType.REPEAT, type))
        template.addCodeBlock(DFCodeBlock.bracket(true, repeating = true))
        for (command in code.nodes) TreeConverter.convertTree(command.tree, template, objects)
        template.addCodeBlock(DFCodeBlock.bracket(false, repeating = true))
    }
}

object StopConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        template.addCodeBlock(DFCodeBlock(DFCodeType.CONTROL, "StopRepeat"))
    }
}

object ReturnConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        template.addCodeBlock(DFCodeBlock(DFCodeType.CONTROL, "Return"))
    }
}

object IsConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
        VariableTracker.setSavedType(tree.left!!.value!! as String, DFVarType.fromId(tree.right!!.value!! as String))
        return TreeConverter.convertTree(tree.left, template, objects)
    }
}