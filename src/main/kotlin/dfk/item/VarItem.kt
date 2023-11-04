package dfk.item

open class VarItem {
    val type: DFVarType
    val value: Any

    constructor(t: DFVarType, v: Any) {
        type = t
        value = v
    }

    override fun toString(): String {
        return value.toString()
    }

    companion object {
        fun num(value: Number): VarItem = num(value.toString())
        fun num(expression: String): VarItem = VarItem(DFVarType.NUMBER, expression)
        fun variable(varName: String, scope: DFVariable.VariableScope = DFVariable.VariableScope.LINE): VarItem = DFVariable(varName, scope)
        fun str(value: String): VarItem = VarItem(DFVarType.STRING, value)
        fun styled(value: String): VarItem = VarItem(DFVarType.STYLED_TEXT, value)
        fun gameValue(name: String, target: String): VarItem = VarItem(DFVarType.GAME_VALUE, mapOf("type" to name, "target" to target))
    }
}