package dfk.item

import kotlin.random.Random
import kotlin.random.nextInt

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
        fun tempVar(): VarItem = variable("Temporary_${nextTempInt()}", DFVariable.VariableScope.LINE)
        fun str(value: String): VarItem = VarItem(DFVarType.STRING, value)
        fun styled(value: String): VarItem = VarItem(DFVarType.STYLED_TEXT, value)
        fun gameValue(name: String, target: String): VarItem = VarItem(DFVarType.GAME_VALUE, mapOf("type" to name, "target" to target))
        fun parameter(name: String, type: DFVarType): VarItem = VarItem(DFVarType.PARAMETER, mapOf("name" to name, "type" to type))
        fun loc(x: Double, y: Double, z: Double, pitch: Float = 0f, yaw: Float = 0f): VarItem = VarItem(DFVarType.LOCATION, mapOf("x" to x, "y" to y, "z" to z, "pitch" to pitch, "yaw" to yaw))

        private var cTempInt = 0
        private fun nextTempInt(): Int {
            return cTempInt++
        }
    }
}