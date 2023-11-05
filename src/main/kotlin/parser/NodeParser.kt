package parser

import dfk.item.DFVariable
import lexer.Token
import lexer.TokenType

class NodeParser(val tokens: MutableList<Token>) {
    var index = 0

    fun parseAssignment(): TreeNode {
        var left = parseEquals()

        while (index < tokens.size && (tokens[index].type == TokenType.ASSIGNMENT)) {
            val operator = tokens[index].type.id
            index++
            val right = parseEquals()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseEquals(): TreeNode {
        var left = parseExpression()

        while (index < tokens.size && (tokens[index].type == TokenType.EQUALS || tokens[index].type == TokenType.NOT_EQUALS || tokens[index].type == TokenType.GREATER_EQUALS || tokens[index].type == TokenType.LESS_EQUALS || tokens[index].type == TokenType.GREATER || tokens[index].type == TokenType.LESS)) {
            val operator = tokens[index].type.id
            index++
            val right = parseExpression()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseExpression(): TreeNode {
        var left = parseTerm()

        while (index < tokens.size && (tokens[index].type == TokenType.ADD || tokens[index].type == TokenType.SUB)) {
            val operator = tokens[index].type.id
            index++
            val right = parseTerm()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseTerm(): TreeNode {
        var left = parseExponent()

        while (index < tokens.size && (tokens[index].type == TokenType.MUL || tokens[index].type == TokenType.DIV || tokens[index].type == TokenType.MOD || tokens[index].type == TokenType.REMAINDER)) {
            val operator = tokens[index].type.id
            index++
            val right = parseExponent()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseExponent(): TreeNode {
        val left = parseAccessor()

        return if (index < tokens.size && (tokens[index].type == TokenType.POW)) {
            val operator = tokens[index].type.id
            index++
            val right = parseExponent()
            TreeNode(operator, left, right)
        } else left
    }

    fun parseAccessor(): TreeNode {
        var left = parseFactor()

        while (index < tokens.size && (tokens[index].type == TokenType.ACCESSOR)) {
            val operator = tokens[index].type.id
            index++
            val right = parseFactor()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseFactor(): TreeNode {
        if (tokens[index].type == TokenType.NUMBER) return parseToken()
        else if (tokens[index].type == TokenType.STRING) return parseToken()
        else if (tokens[index].type == TokenType.STYLED_TEXT) return parseToken()
        else if (tokens[index].type == TokenType.TRUE || tokens[index].type == TokenType.FALSE) return parseTokens()
        else if (tokens[index].type == TokenType.WORD) return parseWord()
        else if (tokens[index].type == TokenType.LOCAL || tokens[index].type == TokenType.GAME || tokens[index].type == TokenType.SAVED) return parseVariableScope()
        else if (tokens[index].type == TokenType.START) return parseStart()
        else if (tokens[index].type == TokenType.IF) return parseIf()

        throw IllegalStateException("Unexpected token: ${tokens[index].type} [$index : ${tokens}]")
    }

    fun parseWord(): TreeNode {
        val wordToken = parseToken()
        if (index < tokens.size && tokens[index].type == TokenType.OPEN_PAREN) {
            index--
            return parseIncDec(parseFunction())
        }
        return parseIncDec(wordToken)
    }

    fun parseStart(): TreeNode {
        index++

        if (index >= tokens.size) throw IllegalStateException("Expected WORD but got nothing")
        if (tokens[index].type != TokenType.WORD) throw IllegalStateException("Expected WORD but got ${tokens[index].type}")
        val procName = tokens[index].value as String

        index++

        var localVarOption = "Don't copy"
        var targetOption = "With current targets"
        for (i in 1..2) {
            if (index < tokens.size) {
                if (tokens[index].type == TokenType.WORD) {
                    if ((tokens[index].value as String) == "copy") {
                        index++
                        localVarOption = "Copy"
                    } else if ((tokens[index].value as String) == "share") {
                        index++
                        localVarOption = "Share"
                    } else if ((tokens[index].value as String) == "foreach") {
                        index++
                        targetOption = "For each in selection"
                    } else if ((tokens[index].value as String) == "none") {
                        index++
                        targetOption = "With no targets"
                    } else if ((tokens[index].value as String) == "selection") {
                        index++
                        targetOption = "With current selection"
                    } else break
                }
            }
        }

        index--

        return TreeNode("start_proc",
            value = procName,
            arguments = mutableListOf(
                TreeNode("lvo", value = localVarOption),
                TreeNode("to", value = targetOption),
            )
        )
    }

    fun parseIf(): TreeNode {
        index++
        val checkExpression = parseTree()
        val ifExpression = parseBlock()

        return TreeNode(
            "cond",
            value = checkExpression,
            left = ifExpression
        )
    }

    fun parseBlock(): TreeNode {
        val t = mutableListOf<Token>()
        var openParens = 0

        while (index < tokens.size) {
            if (tokens[index].type == TokenType.OPEN_PAREN) openParens++
            if (tokens[index].type == TokenType.CLOSE_PAREN) openParens--

            if (openParens == 0) break

            t.add(tokens[index])
            index++
        }
        t.add(tokens[index])
        index++
        println("Left tokens ${tokens.subList(index, tokens.size)}")

        return TreeNode(
            "block",
            value = Parser(t).parseBlock("Block${bCount++}")
        )
    }

    fun parseTree(): TreeNode {
        val t = mutableListOf<Token>()
        var openParens = 0

        while (index < tokens.size) {
            if (tokens[index].type == TokenType.OPEN_PAREN) openParens++
            if (tokens[index].type == TokenType.CLOSE_PAREN) openParens--

            if (openParens == 0) break

            if (tokens[index].type == TokenType.OPEN_PAREN && openParens == 1) {
                index++
                continue
            }

            t.add(tokens[index])
            index++
        }
        index++

        return parseTokens(t)
    }

    fun parseVariableScope(): TreeNode {
        val token = tokens[index]
        index++
        val type = when (token.type) {
            TokenType.GAME -> DFVariable.VariableScope.GAME
            TokenType.LOCAL -> DFVariable.VariableScope.LOCAL
            TokenType.SAVED -> DFVariable.VariableScope.SAVED
            else -> throw UnsupportedOperationException("Unsupported type ${token.type}")
        }
        val wordToken = parseWord()
        wordToken.arguments.add(TreeNode("scope", value = type))
        return wordToken
    }

    fun parseIncDec(tree: TreeNode): TreeNode {
        if (index >= tokens.size) return tree
        if (tokens[index].type == TokenType.INCREMENT_TOKEN) {
            index++
            return parseIncDec(TreeNode("inc", value = tree))
        }
        else if (tokens[index].type == TokenType.DECREMENT_TOKEN) {
            index++
            return parseIncDec(TreeNode("dec", value = tree))
        }
        else return tree
    }

    fun parseFunction(): TreeNode {
        val token = tokens[index]

        val argTokens = mutableListOf<MutableList<Token>>()
        val currentTokens = mutableListOf<Token>()

        index += 2

        var parenCount = 1
        while (index < tokens.size) {
            if (tokens[index].type == TokenType.OPEN_PAREN) parenCount++
            if (tokens[index].type == TokenType.CLOSE_PAREN) parenCount--

            if (parenCount == 0) break

            if (tokens[index].type == TokenType.COMMA) {
                argTokens.add(currentTokens.toTypedArray().toMutableList())
                currentTokens.clear()
                index++
                continue
            }

            currentTokens.add(tokens[index])
            index++
        }
        if (currentTokens.isNotEmpty()) argTokens.add(currentTokens)

        if (index < tokens.size && tokens[index].type == TokenType.CLOSE_PAREN) index++

        val treeArgs = argTokens.map { parseTokens(it) }

        return TreeNode(
            "call",
            value = token.value,
            arguments = treeArgs.toMutableList()
        )
    }

    fun parseToken(): TreeNode {
        val token = tokens[index]
        index++
        return TreeNode(token.type.id, value = token.value)
    }

    fun parseTokens(): TreeNode {
        return parseAssignment()
    }

    companion object {
        fun parseTokens(tokens: MutableList<Token>): TreeNode {
            return NodeParser(tokens).parseTokens()
        }

        private var bCount = 0
    }

}