package lexer

class Lexer(val source: String) {

    fun transform(): MutableList<Token> {
        val disallowedChars = listOf("\\", " ", "\"", "'", "\n", "\t", "\b", "\r")
        val wordAllowedChars = listOf("%", "_")

        val tokens = mutableListOf<Token>()
        var position = 0
        while (position < source.length) {
            when {
                source[position].isDigit() -> {
                    var num = ""
                    var dots = 0
                    while (position < source.length && (source[position].isDigit() || (source[position] == '.' && dots == 0))) {
                        if (source[position] == '.') dots++
                        num += source[position]
                        position++
                    }

                    tokens.add(Token(
                        TokenType.NUMBER,
                        num.toDouble()
                    ))
                }
                source[position] == '$' && (source[position + 1] == '"' || source[position + 1] == '\'') || (source[position] == '"' || source[position] == '\'') -> {
                    val rich = source[position] == '$'
                    if (rich) position++
                    val closingChar = source[position]
                    position++

                    var str = ""
                    var isEscaped = false
                    while (position < source.length && (source[position] != closingChar && !isEscaped)) {
                        var letter = source[position]
                        if (isEscaped) {
                            letter = when (source[position]) {
                                'n' -> '\n'
                                '\\' -> '\\'
                                't' -> 't'
                                'r' -> '\r'
                                '"' -> '"'
                                '\'' -> '\''
                                else -> '\u0000'
                            }
                        }
                        str += letter

                        position++

                        isEscaped = position < source.length && source[position] == '\\'
                        if (isEscaped) position++
                    }
                    position++

                    val tokenType: TokenType
                    if (rich) tokenType = TokenType.STYLED_TEXT
                    else tokenType = TokenType.STRING

                    tokens.add(Token(
                        tokenType,
                        str)
                    )
                }
                source[position].isLetter() || source[position] == '`' -> {
                    val backticks = source[position] == '`'
                    if (backticks) position++

                    var str = ""
                    while (position < source.length) {
                        if (backticks) {
                            if (source[position] == '`') {
                                position++
                                break
                            }
                        }
                        if (!backticks) {
                            if ((!source[position].isLetterOrDigit() && source[position].toString() !in wordAllowedChars) || source[position].toString() in disallowedChars) {
                                break
                            }
                        }
                        str += source[position]
                        position++
                    }

                    var tokenType = TokenType.WORD

                    if (!backticks) {
                        for (type in TokenType.entries) {
                            if (type.word != null && type.word == str) {
                                tokenType = type
                                break
                            }
                        }
                    }

                    tokens.add(Token(
                        tokenType,
                        str
                    ))
                }
                source[position] == '\n' -> {
                    tokens.add(
                        Token(
                        TokenType.NEWLINE,
                        "\n"
                    )
                    )
                    position++
                }
                else -> {
                    var str = ""
                    var added = 0
                    while (position < source.length && !source[position].isLetterOrDigit() && source[position].toString() !in disallowedChars && source[position].toString() !in wordAllowedChars) {
                        str += source[position]
                        position++
                        added++
                    }
                    if (added == 0) position++

                    val typeLengthComparator = Comparator { a: TokenType, b: TokenType ->
                        b.word!!.length - a.word!!.length
                    }

                    val sortedByLengthEntries = TokenType.entries
                        .filter { it.word != null }
                        .sortedWith(typeLengthComparator)

                    for (i in 0..sortedByLengthEntries.size) {
                        for (type in sortedByLengthEntries) {
                            if (str.isEmpty()) break
                            type.word!!

                            val op = type.word
                            var opLen = op.length
                            if (!type.regex) if (!str.startsWith(op)) continue
                            if (type.regex) {
                                val newStr = str.replaceFirst(Regex("^${type.word}"), "")
                                if (newStr == str) continue
                                else {
                                    opLen = str.length - newStr.length
                                }
                            }

                            tokens.add(Token(
                                type,
                                op
                            ))

                            val range = IntRange(0, opLen - 1)
                            str = str.removeRange(range)
                            break
                        }
                    }

                    position -= str.length
                }
            }
        }
        return tokens
    }

}