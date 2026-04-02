package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*
import exspecs.tools.mkStringConst
import java.util.*

fun makeReadln() : TransitionSystem {
    val ctx = Context()
    val initState = State(mapOf<Variable,Value>(
        Pair(Variable("input",stringType),Value("", stringType)),
        Pair(Variable("read",stringType),Value("true", stringType)),
    ))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("PromptUser", listOf(Variable("msg", stringType))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("true")),
            ),
            mapOf(),
            Optional.of { state, act ->
                println(act.lookup(Variable("msg", stringType)))
                val input = readln()
                State(
                    mapOf(
                        Pair(Variable("input", stringType), Value(input, stringType)),
                        Pair(Variable("read", stringType), Value("false", stringType)),
                    )
                )
            }
        ),
        SymbolicAction(
            ActionSignature("Readln", listOf(Variable("msg", stringType))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("false")),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkStringConst("input")),
            ),
            mapOf(Pair(Variable("read", stringType), StringLiteralProgramExpr("true")))
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "ReadProc", ctx, false)
}

fun makeReadlnInt() : TransitionSystem {
    val ctx = Context()
    val initState = State(mapOf(
        Pair(Variable("input", intType),Value(0, intType)),
        Pair(Variable("read", stringType),Value("true", stringType)),
    ))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("PromptUserInt", listOf()),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("true")),
            ),
            mapOf(), //setOf(StringStateVarUpdate(Variable("read","String"), StringUpdateExpr("false"))),
            Optional.of { state, act ->
                println("Enter an Int:")
                val input = readln()
                State(
                    mapOf(
                        Pair(Variable("input", intType), Value(Integer.parseInt(input), intType)),
                        Pair(Variable("read", stringType), Value("false", stringType)),
                    )
                )
            }
        ),
        SymbolicAction(
            ActionSignature("ReadlnInt", listOf(Variable("msg", intType))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("false")),
                ctx.mkEq(ctx.mkIntConst("msg"), ctx.mkIntConst("input")),
            ),
            mapOf(Pair(Variable("read", stringType), StringLiteralProgramExpr("true")))
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "ReadProcInt", ctx, false)
}
