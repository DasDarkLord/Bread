import dfgen.convertAstToDF
import dfk.template.CodeClient
import dfk.template.Recode
import lexer.Lexer
import parser.Parser
import java.io.File
import java.net.ConnectException

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        error("Needs atleast one argument for file")
    }
    println("you MIGHT see a little debug output")

    val text = File(args[0]).readLines().joinToString("\n", "", "")
    val lexer = Lexer(text)
    val tokens = lexer.transform()
    println(tokens)

    val parser = Parser(tokens)
    val events = parser.parseAll()

    for (event in events) println(event)

    val templates = convertAstToDF(events)

    for (template in templates) {
        println(template.getJson())
    }

    try { CodeClient.buildTemplate(templates) } catch (ignored: ConnectException) { }
//    try { Recode.sendTemplate(templates) } catch (ignored: ConnectException) { }
}