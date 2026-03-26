package exspecs.cli

import com.microsoft.z3.*
import exspecs.ast.ASTBuilder
import exspecs.ast.buildAST
import exspecs.parser.ExspecLexer
import exspecs.parser.ExspecParser
import exspecs.parser.ExspecParserBaseVisitor
import exspecs.parser.ExspecParserListener
import exspecs.program.*
import exspecs.tools.mkStringConst
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun makeTS1() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(
        IntVarAssignment(Variable("i","Int"), 0),
        StringVarAssignment(Variable("p","String"), "false"),
    ))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("I", listOf(Variable("inc","Int"))),
            ctx.mkAnd(
                ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)),
                ctx.mkEq(ctx.mkStringConst("p"), ctx.mkString("false")),
                ctx.mkGt(ctx.mkIntConst("inc"), ctx.mkInt(2)),
            ),
            setOf(
                IntStateVarUpdate(
                    Variable("i","Int"),
                    PlusIntUpdateExpr(IntVarUpdateExpr(Variable("i","Int")), IntVarUpdateExpr(Variable("inc","Int")))
                ),
                StringStateVarUpdate(
                    Variable("p","String"),
                    StringUpdateExpr("true")
                ),
            )
        ),
        SymbolicAction(
            ActionSignature("Println", listOf(Variable("msg","Int"))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkIntConst("msg"),ctx.mkIntConst("i")),
                ctx.mkOr(
                    ctx.mkEq(ctx.mkStringConst("p"), ctx.mkString("true")),
                    ctx.mkAnd(ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)), ctx.mkEq(ctx.mkStringConst("p"), ctx.mkString("true"))),
                ),
            ),
            setOf(
                StringStateVarUpdate(
                    Variable("p","String"),
                    StringUpdateExpr("false")
                ),
            )
        ),
    )
    return GenericTransitionSystem(initState, alphabet, "P1", ctx)
}

fun makeTS2() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(IntVarAssignment(Variable("i","Int"), 0)))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("I", listOf(Variable("inc","Int"))),
            ctx.mkAnd(
                ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)),
                ctx.mkEq(ctx.mkMod(ctx.mkIntConst("inc"), ctx.mkInt(2)), ctx.mkInt(0))
            ),
            setOf(
                IntStateVarUpdate(
                    Variable("i","Int"),
                    PlusIntUpdateExpr(IntVarUpdateExpr(Variable("i","Int")), IntVarUpdateExpr(Variable("inc","Int")))
                ),
            )
        ),
    )
    return GenericTransitionSystem(initState, alphabet, "P2", ctx)
}

fun main(args : Array<String>) {
    /*
    println("Program 1:")
    Program(setOf(makeTS1(), makeTS2())).testRun()
    println()
    println("Program 2:")
    Program(setOf(makeTS1())).run()
     */

    //val input = CharStreams.fromString("p-class S {}")
    val input = CharStreams.fromFileName("/Users/idardik/Documents/CMU/exspecs/java/Exspecs/input/test1.jul")
    val ast = buildAST(input)
    println(ast)
}
