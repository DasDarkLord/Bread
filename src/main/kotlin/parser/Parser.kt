package parser

import dfgen.WordConverter
import dfk.item.DFVarType
import dfk.item.DFVariable
import lexer.Token
import lexer.TokenType

class Parser(val input: MutableList<Token>) {

    var pointer = -1

    fun hasNext(): Boolean {
        return pointer + 1 < input.size
    }

    fun peek(): Token {
        if (hasNext()) return input[pointer + 1]
        throw IllegalStateException("Expected something but got nothing !?")
    }

    fun next(): Token {
        val token = peek()
        pointer++
        return token
    }

    fun parseAll(): List<Ast.Event> {
        val output = mutableListOf<Ast.Event>()
        while (hasNext()) {
            if (peek().type == TokenType.LOCAL || peek().type == TokenType.GAME || peek().type == TokenType.SAVED) {
                val type = next()
                val word = next()
                if (word.type != TokenType.WORD) throw IllegalStateException("Expected word but got ${word.type}")
                val scope = when (type.type) {
                    TokenType.LOCAL -> DFVariable.VariableScope.LOCAL
                    TokenType.GAME -> DFVariable.VariableScope.GAME
                    TokenType.SAVED -> DFVariable.VariableScope.SAVED
                    else -> throw UnsupportedOperationException("Expected local, game or save but got ${type.type}")
                }
                WordConverter.wordScopes[word.value as String] = scope
            } else {
                if (peek().type != TokenType.LOCAL && peek().type != TokenType.GAME && peek().type != TokenType.SAVED) break
            }
        }
        while (hasNext()) output.add(parseEvent())
        return output
    }

    fun parseEvent(): Ast.Event {
        val eventToken = next()

        if (eventToken.type != TokenType.EVENT && eventToken.type != TokenType.FUNCTION && eventToken.type != TokenType.PROCESS) throw IllegalStateException("Expected one of event, function or process but got ${eventToken.type} [at $pointer (${input[pointer]}) in $input]")
        val nameToken = next()

        if (nameToken.type != TokenType.WORD) throw IllegalStateException("Expected word but got ${nameToken.type}")

        if (eventToken.type == TokenType.FUNCTION) {
            val paramMap = mutableMapOf<String, DFVarType>()
            next()
            while (hasNext() && peek().type != TokenType.CLOSE_PAREN) {
                val paramName = next()
                var varType = DFVarType.ANY
                if (peek().type == TokenType.COLON) {
                    next()
                    varType = DFVarType.fromId(next().value as String)
                }

                paramMap[paramName.value as String] = varType

                if (peek().type == TokenType.COMMA) next()
            }
            if (peek().type == TokenType.CLOSE_PAREN) next()

            val block = parseBlock(nameToken.value as String)

            return Ast.Event(nameToken.value as String, block, EventType.Function(paramMap))
        } else if (eventToken.type == TokenType.PROCESS) {
            return Ast.Event(nameToken.value as String, parseBlock(nameToken.value as String), EventType.Process)
        }
        else return Ast.Event(nameToken.value as String, parseBlock(nameToken.value as String), EventType.Event)
    }

    fun parseBlock(eventName: String): Ast.Block {
        val openParen = next()
        if (openParen.type != TokenType.OPEN_CURLY) throw IllegalStateException("Expected open curly but got ${openParen.type} at $pointer $openParen")
        next()

        val commands = mutableListOf<Ast.Command>()

        while (input[pointer].type != TokenType.CLOSE_CURLY) {
            val command = parseCommand()
            commands.add(command)
        }

        val closingParen = input[pointer]
        if (closingParen.type != TokenType.CLOSE_CURLY) throw IllegalStateException("Expected close curly but got ${closingParen.type}")

        return Ast.Block(commands, eventName)
    }

    fun parseCommand(): Ast.Command {
        val hasParens = input[pointer].type == TokenType.OPEN_PAREN
        if (hasParens) next()

        val tree: TreeNode
        if (hasParens) {
            val commandTokens = mutableListOf<Token>()
            var parenCount = 1
            while (hasNext()) {
                val token = next()
                if (token.type == TokenType.OPEN_PAREN) parenCount++
                if (token.type == TokenType.CLOSE_PAREN) parenCount--
                if (parenCount == 0) break

                commandTokens.add(token)
            }
            next()

            println(commandTokens)
            tree = NodeParser.parseTokens(commandTokens)
        } else {
            val parserTokens = input.subList(pointer, input.size)
            val parser = NodeParser(parserTokens)
            println(parserTokens)
            tree = parser.parseTokens()
            pointer += parser.index
//            if (input[pointer].type == TokenType.IF) pointer--
        }

        println(tree)
        return Ast.Command(tree)
    }

}