package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*
import java.util.*

fun makePrintln() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf())
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("Println", listOf(Variable("msg","String"))),
            ctx.mkTrue(),
            setOf(),
            Optional.of { state, act -> println(act.lookup(Variable("msg", "String"))); state }
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "PrintProc", ctx, false)
}

fun makePrintlnInt() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf())
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("PrintlnInt", listOf(Variable("msg","Int"))),
            ctx.mkTrue(),
            setOf(),
            Optional.of { state, act -> println(act.lookup(Variable("msg", "Int"))); state }
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "PrintProcInt", ctx, false)
}
