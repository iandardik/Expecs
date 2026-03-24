package exspecs.program.library

import com.microsoft.z3.Context
import exspecs.program.*

fun makePrintln() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf())
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("Println", listOf("msg")),
            ctx.mkTrue(),
            setOf(),
            setOf({ _,act -> println(act.lookupInt("msg")) })
        ),
    )
    // set selfTerminate to false because this is a library function
    return GenericTransitionSystem(initState, alphabet, "PrintProc", ctx, false)
}
