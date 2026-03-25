package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*

fun makePrintln() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf())
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("Println", listOf(Variable("msg","Int"))), // TODO make the type a String
            ctx.mkTrue(),
            setOf(),
            setOf({ _,act -> println(act.lookup(Variable("msg","Int"))) })
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "PrintProc", ctx, false)
}
