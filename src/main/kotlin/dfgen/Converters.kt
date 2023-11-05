package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.DFVarType
import dfk.item.DFVariable
import dfk.item.VarItem
import dfk.template.DFTemplate
import parser.Ast
import parser.Parser
import parser.TreeNode
import java.util.StringJoiner

val astConverters = mapOf(
    "number" to NumberConverter, "word" to WordConverter, "string" to StringConverter, "styled" to StyledTextConverter,
    "true" to TrueConverter, "false" to FalseConverter,
    "assign" to AssignmentConverter, "acc" to AccessorConverter,
    "add" to AdditionConverter, "sub" to SubtractionConverter, "mul" to MultiplicationConverter, "div" to DivisionConverter, "pow" to ExponentConverter, "rem" to RemainderConverter, "mod" to ModuloConverter,
    "inc" to IncrementConverter, "dec" to DecrementConverter,
    "call" to CallConverter, "start_proc" to StartProcConverter,
    "cond" to IfConverter
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

object AdditionConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        var left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        var right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        val rTypes = listOf(DFVarType.STYLED_TEXT, DFVarType.STRING, DFVarType.VARIABLE, DFVarType.NUMBER)
        if (left.type !in rTypes) {
            val tempVar = VarItem.tempVar()
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "=").setContent(tempVar, left))
            left = tempVar
        }
        if (right.type !in rTypes) {
            val tempVar = VarItem.tempVar()
            template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "=").setContent(tempVar, right))
            right = tempVar
        }
        if (left.type == DFVarType.STYLED_TEXT || right.type == DFVarType.STYLED_TEXT) {
            return VarItem.styled("${left}${right}")
        }
        if (left.type == DFVarType.STRING || right.type == DFVarType.STRING) {
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
        return variable
    }
}

object IncrementConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val variable = TreeConverter.convertTree(tree.value!! as TreeNode, template, objects) as VarItem
        template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "+=")
            .setContent(
                variable
            )
        )
        return variable
    }
}

object DecrementConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val variable = VarItem.tempVar()
        template.addCodeBlock(DFCodeBlock(DFCodeType.SET_VARIABLE, "-=")
            .setContent(
                variable,
                TreeConverter.convertTree(tree.value!! as TreeNode, template, objects) as VarItem
            )
        )
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

                return TreeConverter.convertTree(tree.left, template, objects) as VarItem
            }
            else throw UnsupportedOperationException("Expected a value or condition but got $right")
        } else {
            val rightVarItem = right as? VarItem ?: VarItem.num(0)
            template.addCodeBlock(
                DFCodeBlock(DFCodeType.SET_VARIABLE, "=")
                    .setContent(left, rightVarItem)
            )
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
            template.addCodeBlock(DFCodeBlock(DFCodeType.CALL_FUNCTION, tree.value as String)
                .setContent(*tree.arguments.map { TreeConverter.convertTree(it, template, objects) as VarItem }.toTypedArray()))
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
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        val code = tree.left!!.value!! as Ast.Block
        val check = tree.value!! as TreeNode

        val cbs = template.codeBlocks.size
        try {
            val v = TreeConverter.convertTree(check, template, objects)
            if (v is CustomIf) {
                v.customIf(code, tree, template, objects)
                return
            } else throw java.lang.Exception()
        } catch (ignored: Exception) {
            var added = template.codeBlocks.size - cbs
            while (added > 0) {
                template.codeBlocks.removeAt(template.codeBlocks.size)
                added--
            }
        }

        val action = when (check.type) {
            "eq" -> "="
            "neq" -> "!="
            "lt" -> "<"
            "gt" -> ">"
            "geq" -> ">="
            "leq" -> "<="
            else -> "?"
        }
        var ifCodeBlock = DFCodeBlock(DFCodeType.IF_VARIABLE, action)
        if (action == "?") { // Assume it's  `if (boolVariable)` syntax
            ifCodeBlock = ifCodeBlock.setContent(TreeConverter.convertTree(check, template, objects) as VarItem, TrueConverter.convert(tree, template, objects))
            ifCodeBlock.action = "="
        } else {
            ifCodeBlock = ifCodeBlock.setContent(TreeConverter.convertTree(check.left!!, template, objects) as VarItem, TreeConverter.convertTree(check.right!!, template, objects) as VarItem)
        }

        template.addCodeBlock(ifCodeBlock)
        template.addCodeBlock(DFCodeBlock.bracket(true))
        for (command in code.nodes) TreeConverter.convertTree(command.tree, template, objects)
        template.addCodeBlock(DFCodeBlock.bracket(false))
    }
}