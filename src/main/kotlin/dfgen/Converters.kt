package dfgen

import dfk.codeblock.DFCodeBlock
import dfk.codeblock.DFCodeType
import dfk.item.DFVarType
import dfk.item.DFVariable
import dfk.item.VarItem
import dfk.template.DFTemplate
import parser.TreeNode
import kotlin.random.Random
import kotlin.random.nextInt

val astConverters = mapOf(
    "number" to NumberConverter, "word" to WordConverter, "string" to StringConverter, "styled" to StyledTextConverter,
    "assign" to AssignmentConverter, "acc" to AccessorConverter,
    "add" to AdditionConverter, "sub" to SubtractionConverter, "mul" to MultiplicationConverter, "div" to DivisionConverter, "pow" to ExponentConverter
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

object AdditionConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        val left = TreeConverter.convertTree(tree.left!!, template, objects) as VarItem
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem
        if (left.type == DFVarType.STRING || right.type == DFVarType.STRING) {
            return VarItem.str("${left}${right}")
        }
        if (left.type == DFVarType.STYLED_TEXT || right.type == DFVarType.STYLED_TEXT) {
            return VarItem.styled("${left}${right}")
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
        val variable = VarItem.variable("${Random.nextInt(Int.MIN_VALUE..Int.MAX_VALUE)}", DFVariable.VariableScope.LINE)
        template.addCodeBlock(DFCodeBlock(
            DFCodeType.SET_VARIABLE,
            "Exponent"
        ).setContent(variable, left, right))
        return variable
    }
}

object AssignmentConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>) {
        val right = TreeConverter.convertTree(tree.right!!, template, objects) as VarItem

        template.addCodeBlock(
            DFCodeBlock(DFCodeType.SET_VARIABLE, "=")
                .setContent(WordConverter.convert(tree.left!!, template, objects), right)
        )
    }
}

object WordConverter : AstConverter {
    val wordScopes = mutableMapOf<String, DFVariable.VariableScope>()
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): VarItem {
        var scope = DFVariable.VariableScope.LINE
        if (wordScopes.containsKey(tree.value!! as String)) scope = wordScopes[tree.value as String]!!
        if (tree.arguments.isNotEmpty()) {
            scope = tree.arguments[0].value!! as DFVariable.VariableScope
        }
        wordScopes[tree.value as String] = scope
        return VarItem.variable(tree.value, scope)
    }
}

object AccessorConverter : AstConverter {
    override fun convert(tree: TreeNode, template: DFTemplate, objects: MutableMap<String, DFLObject>): Any? {
        val name = tree.left!!.value!! as String
        val right = tree.right!!

        val obj = objects[name]!!

        if (right.type == "call") {
            return obj.accessFunc(right.value!! as String)!!.convert(right, template, objects)
        } else {
            return obj.accessField(right.value!! as String)!!.convert(right, template, objects)
        }
    }

}