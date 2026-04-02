package exspecs.cli

import exspecs.ast.ProgramNode
import exspecs.ast.buildAST
import org.antlr.v4.runtime.CharStreams

fun main(args : Array<String>) {
    if (args.size != 1) {
        println("usage: Exspec <.jul file>")
        return
    }
    val input = CharStreams.fromFileName(args[0])
    val ast = buildAST(input)
    //println(ast)
    val programAST = ast as ProgramNode
    val typedAST = programAST.toTypedAST()
    val prog = typedAST.toProgram()
    prog.run()
}
