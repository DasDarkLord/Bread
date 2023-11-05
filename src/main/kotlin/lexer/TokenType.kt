package lexer

enum class TokenType {
    // Types
    NUMBER("number"),
    STRING("string"),
    STYLED_TEXT("styled"),
    WORD("word"),
    TRUE("true", "true"),
    FALSE("false", "false"),

    // Keywords
    IF("if", "if"),
    EVENT("event", "event"),
    FUNCTION("func", "func"),
    PROCESS("proc", "proc"),
    GAME("game", "game"),
    LOCAL("local", "local"),
    SAVED("save", "saved"),

    // Operators
    ASSIGNMENT("assign", "="),
    EQUALS("eq", "=="),
    NOT_EQUALS("neq", "!="),
    GREATER_EQUALS("geq", ">="),
    LESS_EQUALS("leq", "<="),
    GREATER("gt", ">"),
    LESS("lt", "<"),
    INCREMENT_TOKEN("inc", "++"),
    DECREMENT_TOKEN("dec", "--"),
    ADD("add", "+"),
    SUB("sub", "-"),
    MUL("mul", "*"),
    DIV("div", "/"),
    POW("pow", "^"),
    MOD("mod", "mod"),
    REMAINDER("rem", "rem"),

    // Misc
    OPEN_PAREN("oparen", "[(\\[{]", true),
    CLOSE_PAREN("cparen", "[)\\]}]", true),
    ACCESSOR("acc", "."),
    COMMA("comma", ","),
    START("start_proc", "start"),
    ;

    val id: String
    val word: String?
    val regex: Boolean
    constructor(id: String, word: String? = null, regex: Boolean = false) {
        this.id = id
        this.word = word
        this.regex = regex
    }
}