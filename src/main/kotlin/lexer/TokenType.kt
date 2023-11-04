package lexer

enum class TokenType {
    // Types
    NUMBER("number"),
    STRING("string"),
    STYLED_TEXT("styled"),
    WORD("word"),

    // Keywords
    IF("if", "if"),
    EVENT("event", "event"),
    FUNCTION("func", "function"),
    GAME("game", "game"),
    LOCAL("local", "local"),
    SAVED("save", "saved"),

    // Operators
    ASSIGNMENT("assign", "="),
    ADD("add", "+"),
    SUB("sub", "-"),
    MUL("mul", "*"),
    DIV("div", "/"),
    POW("pow", "^"),

    // Misc
    OPEN_PAREN("oparen", "[(\\[{]", true),
    CLOSE_PAREN("cparen", "[)\\]}]", true),
    ACCESSOR("acc", "."),
    COMMA("comma", ","),
    NEWLINE("newline"),
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