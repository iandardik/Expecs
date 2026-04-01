package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*
import exspecs.tools.mkStringConst
import java.util.*

fun makeReadln() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(
        StringVarAssignment(Variable("input","String"),""),
        StringVarAssignment(Variable("read","String"),"true"),
    ))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("PromptUser", listOf(Variable("msg","String"))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("true")),
            ),
            setOf(),
            Optional.of { state, act ->
                println(act.lookup(Variable("msg","String")))
                val input = readln()
                State(
                    setOf(
                        StringVarAssignment(Variable("input", "String"), input),
                        StringVarAssignment(Variable("read", "String"), "false"),
                    )
                )
            }
        ),
        SymbolicAction(
            ActionSignature("Readln", listOf(Variable("msg","String"))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("false")),
                ctx.mkEq(ctx.mkStringConst("msg"), ctx.mkStringConst("input")),
            ),
            setOf(StringStateVarUpdate(Variable("read","String"), StringUpdateExpr("true")))
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "ReadProc", ctx, false)
}

fun makeReadlnInt() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(
        IntVarAssignment(Variable("input","Int"),0),
        StringVarAssignment(Variable("read","String"),"true"),
    ))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("PromptUserInt", listOf()),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("true")),
            ),
            setOf(), //setOf(StringStateVarUpdate(Variable("read","String"), StringUpdateExpr("false"))),
            Optional.of { state, act ->
                println("Enter an Int:")
                val input = readln()
                State(
                    setOf(
                        IntVarAssignment(Variable("input", "Int"), Integer.parseInt(input)),
                        StringVarAssignment(Variable("read", "String"), "false"),
                    )
                )
            }
        ),
        SymbolicAction(
            ActionSignature("ReadlnInt", listOf(Variable("msg","Int"))),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkStringConst("read"), ctx.mkString("false")),
                ctx.mkEq(ctx.mkIntConst("msg"), ctx.mkIntConst("input")),
            ),
            setOf(StringStateVarUpdate(Variable("read","String"), StringUpdateExpr("true")))
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "ReadProcInt", ctx, false)
}
