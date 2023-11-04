package parser

import dfk.item.DFVariable
import lexer.Token
import lexer.TokenType

class NodeParser(val tokens: MutableList<Token>) {
    var index = 0

    fun parseAssignment(): TreeNode {
        var left = parseAccessor()

        while (index < tokens.size && (tokens[index].type == TokenType.ASSIGNMENT)) {
            val operator = tokens[index].type.id
            index++
            val right = parseAccessor()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseAccessor(): TreeNode {
        var left = parseExpression()

        while (index < tokens.size && (tokens[index].type == TokenType.ACCESSOR)) {
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

        while (index < tokens.size && (tokens[index].type == TokenType.MUL || tokens[index].type == TokenType.DIV)) {
            val operator = tokens[index].type.id
            index++
            val right = parseExponent()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseExponent(): TreeNode {
        var left = parseFactor()

        while (index < tokens.size && (tokens[index].type == TokenType.POW)) {
            val operator = tokens[index].type.id
            index++
            val right = parseFactor()
            left = TreeNode(operator, left, right)
        }

        return left
    }

    fun parseFactor(): TreeNode {
        if (tokens[index].type == TokenType.NUMBER) return parseToken()
        if (tokens[index].type == TokenType.STRING) return parseToken()
        if (tokens[index].type == TokenType.STYLED_TEXT) return parseToken()
        else if (tokens[index].type == TokenType.WORD) return parseWord()
        else if (tokens[index].type == TokenType.LOCAL || tokens[index].type == TokenType.GAME || tokens[index].type == TokenType.SAVED) return parseScope()

        throw IllegalStateException("Unexpected token: ${tokens[index].type}")
    }

    fun parseWord(): TreeNode {
        val wordToken = parseToken()
        if (index < tokens.size && tokens[index].type == TokenType.OPEN_PAREN) {
            index--
            return parseFunction()
        }
        return wordToken
    }

    fun parseScope(): TreeNode {
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

    companion object {
        fun parseTokens(tokens: MutableList<Token>): TreeNode {
            return NodeParser(tokens).parseAssignment()
        }
    }

}