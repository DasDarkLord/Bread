package dfk.codeblock

enum class DFCodeType {
    PLAYER_EVENT("event", "Player Event", false, false),
    PLAYER_ACTION("player_action", "Player Action",  false, true),
    ENTITY_EVENT("entity_event", "Entity Event", false, false),
    ENTITY_ACTION("entity_action", "Entity Action",  false, true),
    SET_VARIABLE("set_var", "Set Variable", false, true),
    GAME_ACTION("game_action", "Game Action", false, true),
    IF_VARIABLE("if_var", "If Variable", true, true),
    REPEAT("repeat", "Repeat", true, true),
    IF_GAME("if_game", "If Game", true, true),
    IF_PLAYER("if_player", "If Player", true, true),
    IF_ENTITY("if_entity", "If Entity", true, true),
    ELSE("else", "Else", true, false),
    FUNCTION("func", "Function", false, true),
    PROCESS("process", "Process", false, true),
    CALL_FUNCTION("call_func", "Call Function", false, false),
    START_PROCESS("start_process", "Start Process", false, true),
    CONTROL("control", "Control", false, true),
    SELECT_OBJECT("select_obj", "Select Object", false, true),
    BRACKET("bracket", "Bracket", false, false);

    val jsonName: String
    val blockName: String
    val hasBrackets: Boolean
    val hasChest: Boolean

    constructor(jn: String, bn: String, b: Boolean, c: Boolean) {
        jsonName = jn
        blockName = bn
        hasBrackets = b
        hasChest = c
    }
}