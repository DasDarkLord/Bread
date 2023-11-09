import dfgen.convertAstToDF
import dfk.item.DFVarType
import dfk.template.CodeClient
import dfk.template.DFTemplate
import dfk.template.Recode
import lexer.Lexer
import lexer.Token
import parser.Parser
import parser.preprocessImports
import parser.preprocessTemplates
import java.io.File
import java.net.ConnectException

val pairs = mutableListOf<Pair<String, MutableMap<String, DFVarType>>>()

fun fullLex(str: String): Pair<MutableList<Token>, List<DFTemplate>> {
    val lexer = Lexer(str)

    val tokens = lexer.transform()
    println(tokens)

    val templates = mutableListOf<DFTemplate>()

    val importProcessed = preprocessImports(tokens)
    templates.addAll(importProcessed.second)

    val templateProcessed = preprocessTemplates(importProcessed.first)
    println(templateProcessed)

    val templateProcessedTokens = templateProcessed.first
    templates.addAll(templateProcessed.second)
    println(templateProcessedTokens)

    return Pair(templateProcessedTokens, templates)
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        error("Needs atleast one argument for file")
    }
    println("you MIGHT see a little debug output")

    val text = File(args[0]).readLines().joinToString("\n", "", "")
    val pair = fullLex(text)
    val tokens = pair.first
    val impTemplates = pair.second

    val parser = Parser(tokens)
    pairs.clear()
    pairs.addAll(parser.getFunctions())
    println("Read $pairs")

    val events = parser.parseAll()

    for (event in events) println(event)

    val templates = convertAstToDF(events).toMutableList()
    templates.addAll(impTemplates)

    for (template in templates) {
        println(template.getJson())
    }

    try { CodeClient.buildTemplate(templates) } catch (ignored: ConnectException) { }
}