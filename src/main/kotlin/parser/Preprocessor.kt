package parser

import dfk.template.DFTemplate
import fullLex
import lexer.Token
import lexer.TokenType
import java.io.File
import java.util.Base64

fun preprocessImports(tokens: List<Token>, disallowedImports: MutableList<String> = mutableListOf()): Pair<MutableList<Token>, MutableList<DFTemplate>> {
    val added = mutableListOf<Token>()
    val removeIndexes = mutableListOf<Int>()

    val templates = mutableListOf<DFTemplate>()
    for ((index, token) in tokens.withIndex()) {
        if (token.type == TokenType.IMPORT) {
            removeIndexes.add(index)
            removeIndexes.add(index + 1)
            val f = tokens[index + 1].value as String
            if (f in disallowedImports) continue
            disallowedImports.add(f)

            for (file in File("./").listFiles()!!) {
                if (file.name == f || file.nameWithoutExtension == f) {
                    val pair = fullLex(file.readLines().joinToString("\n", "", ""), disallowedImports)
                    added.addAll(pair.first)
                    templates.addAll(pair.second)
                    break
                }
            }
        }
    }

    val filteredTokens = tokens.toMutableList()
    for (index in removeIndexes.sortedDescending()) {
        filteredTokens.removeAt(index)
    }
    filteredTokens.addAll(0, added)

    return Pair(filteredTokens, templates)
}

fun preprocessTemplates(tokens: List<Token>): Pair<MutableList<Token>, MutableList<DFTemplate>> {
    val removeIndexes = mutableListOf<Int>()

    val templates = mutableListOf<DFTemplate>()
    for ((index, token) in tokens.withIndex()) {
        if (token.type == TokenType.TEMPLATE) {
            removeIndexes.add(index)
            removeIndexes.add(index + 1)
            val template = DFTemplate.decompressGzip(Base64.getDecoder().decode(tokens[index + 1].value as String))
            templates.add(DFTemplate.fromJson(template))
        }
    }

    val filteredTokens = tokens.toMutableList()
    for (index in removeIndexes.sortedDescending()) {
        filteredTokens.removeAt(index)
    }

    return Pair(filteredTokens, templates)
}