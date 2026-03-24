package exspecs.cli

import com.microsoft.z3.*
import exspecs.program.*

fun makeTS1() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(IntVarAssignment(VarName("i"), 0)))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("I", listOf("inc")),
            ctx.mkAnd(
                ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)),
                ctx.mkGt(ctx.mkIntConst("inc"), ctx.mkInt(2))
            ),
            setOf(
                IntStateVarUpdate(
                    VarName("i"),
                    PlusIntUpdateExpr(IntVarUpdateExpr(VarName("i")), IntVarUpdateExpr(VarName("inc")))
                ),
            )
        )
    )
    return GenericTransitionSystem(initState, alphabet, "P1", ctx)
}

fun makeTS2() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(IntVarAssignment(VarName("i"), 0)))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("I", listOf("inc")),
            ctx.mkAnd(
                ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)),
                ctx.mkEq(ctx.mkMod(ctx.mkIntConst("inc"), ctx.mkInt(2)), ctx.mkInt(0))
            ),
            setOf(
                IntStateVarUpdate(
                    VarName("i"),
                    PlusIntUpdateExpr(IntVarUpdateExpr(VarName("i")), IntVarUpdateExpr(VarName("inc")))
                ),
            )
        )
    )
    return GenericTransitionSystem(initState, alphabet, "P2", ctx)
}

fun main(args : Array<String>) {
    val prog = Program(setOf(makeTS1(), makeTS2()))
    prog.run()
}
