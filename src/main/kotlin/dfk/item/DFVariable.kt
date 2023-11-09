package dfk.item

class DFVariable(varName: String, varScope: VariableScope) : VarItem(DFVarType.VARIABLE, mapOf("name" to varName, "scope" to varScope)) {

    enum class VariableScope(val jsonName: String) {
        LOCAL("local"),
        GAME("unsaved"),
        LINE("line"),
        SAVED("saved");

        companion object {
            fun fromId(id: String): VariableScope {
                for (entry in entries) {
                    if (entry.jsonName.lowercase() == id.lowercase()) return entry
                }
                return LINE
            }
        }
    }

    override fun toString(): String {
        return "%var(${(value as Map<*, *>)["name"]})"
    }

}