import dfgen.convertAstToDF
import dfk.template.CodeClient
import lexer.Lexer
import parser.Parser
import java.io.File
import java.net.ConnectException

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        error("Needs atleast one argument for file")
    }
    val text = File(args[0]).readLines().joinToString("\n", "", "")
    val lexer = Lexer(text)
    val tokens = lexer.transform()
    println(tokens)

    val parser = Parser(tokens)
    val events = parser.parseAll()

    println(events)

    val templates = convertAstToDF(events)

    for (template in templates) {
        println(template.getJson())
    }

    try { CodeClient.sendTemplate(templates) } catch (ignored: ConnectException) { }
    // Recode.sendTemplate doesn't work
}