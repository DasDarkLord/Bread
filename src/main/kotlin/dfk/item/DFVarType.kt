package dfk.item

import com.sun.org.apache.xerces.internal.impl.dv.xs.AnySimpleDV

enum class DFVarType {
    STRING("txt"),
    STYLED_TEXT("comp"),
    NUMBER("num"),
    VARIABLE("var"),
    LOCATION("loc"),
    ITEM("item"),
    ANY("any"),
    GAME_VALUE("g_val"),
    VECTOR("vec"),
    POTION("pot"),
    SOUND("snd"),
    PART("part"),
    PARAMETER("pn_el"),
    LIST("list"),
    DICT("dict");

    val id: String
    constructor(i: String) {
        id = i
    }

    companion object {
        fun fromId(id: String): DFVarType {
            val altNames = mapOf(
                "str" to "txt",
                "string" to "txt",
                "styled" to "comp",
                "sound" to "snd",
                "particle" to "part",
                "potion" to "pot",
                "vector" to "vec",
                "number" to "num",
                "variable" to "var",
                "dictionary" to "dict",
            )
            val newId = altNames[id] ?: id

            for (t in entries) {
                if (t.id == newId) return t
            }
            return ANY
        }
    }

}