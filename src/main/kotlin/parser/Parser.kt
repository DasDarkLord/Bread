package parser

import dfgen.WordConverter
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
            } else if (peek().type == TokenType.NEWLINE) next()
            else break
        }
        while (hasNext()) output.add(parseEvent())
        return output
    }

    private fun parseEvent(): Ast.Event {
        var eventToken = next()
        while (eventToken.type == TokenType.NEWLINE) eventToken = next()

        val nameToken = next()

        if (eventToken.type != TokenType.EVENT) throw IllegalStateException("Expected event but got ${eventToken.type}")
        if (nameToken.type != TokenType.WORD) throw IllegalStateException("Expected word but got ${nameToken.type}")

        return Ast.Event(nameToken.value as String, parseBlock(nameToken.value), EventType.Event)
    }

    private fun parseBlock(eventName: String): Ast.Block {
        val openParen = next()
        if (openParen.type != TokenType.OPEN_PAREN) throw IllegalStateException("Expected open paren but got ${openParen.type}")
        val commands = mutableListOf<Ast.Command>()

        if (peek().type == TokenType.NEWLINE) next()

        while (peek().type != TokenType.CLOSE_PAREN) {
            val command = parseCommand()
            commands.add(command)
        }
        val closeParen = next()
        if (closeParen.type != TokenType.CLOSE_PAREN) throw IllegalStateException("Expected close paren but got ${closeParen.type}")

        return Ast.Block(commands, eventName)
    }

    private fun parseCommand(): Ast.Command {
        val hasParens = peek().type == TokenType.OPEN_PAREN
        if (hasParens) next()

        val commandTokens = mutableListOf<Token>()
        if (hasParens) {
            var parenCount = 1
            while (hasNext()) {
                val token = next()
                if (token.type == TokenType.OPEN_PAREN) parenCount++
                if (token.type == TokenType.CLOSE_PAREN) parenCount--
                if (parenCount == 0) break

                commandTokens.add(token)
            }
        } else {
            while (hasNext()) {
                val token = next()
                if (token.type == TokenType.NEWLINE) break
                commandTokens.add(token)
            }
        }

        println(commandTokens)

        return Ast.Command(NodeParser.parseTokens(commandTokens))
    }

}