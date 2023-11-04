package dfk.item

enum class DFVarType {
    STRING("txt"),
    STYLED_TEXT("comp"),
    NUMBER("num"),
    VARIABLE("var"),
    LOCATION("loc"),
    ITEM("item"),
    ANY("txt"),
    GAME_VALUE("g_val"),
    VECTOR("vec"),
    POTION("pot"),
    SOUND("snd"),
    PART("part");

    val id: String
    constructor(i: String) {
        id = i
    }

}