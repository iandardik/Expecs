package exspecs.cli

import com.microsoft.z3.*
import exspecs.program.*

fun makeTS1() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf(
        IntVarAssignment(VarName("i"), 0),
        IntVarAssignment(VarName("p"), 0),
    ))
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("I", listOf("inc")),
            ctx.mkAnd(
                ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)),
                ctx.mkEq(ctx.mkIntConst("p"), ctx.mkInt(0)),
                ctx.mkGt(ctx.mkIntConst("inc"), ctx.mkInt(2)),
            ),
            setOf(
                IntStateVarUpdate(
                    VarName("i"),
                    PlusIntUpdateExpr(IntVarUpdateExpr(VarName("i")), IntVarUpdateExpr(VarName("inc")))
                ),
                IntStateVarUpdate(
                    VarName("p"),
                    IntUpdateExpr(1)
                ),
            )
        ),
        SymbolicAction(
            ActionSignature("Print", listOf("iVal")),
            ctx.mkAnd(
                ctx.mkEq(ctx.mkIntConst("iVal"),ctx.mkIntConst("i")),
                ctx.mkOr(
                    ctx.mkLe(ctx.mkIntConst("i"), ctx.mkInt(10)),
                    ctx.mkEq(ctx.mkIntConst("p"), ctx.mkInt(1)),
                ),
            ),
            setOf(
                IntStateVarUpdate(
                    VarName("p"),
                    IntUpdateExpr(0)
                ),
            )
        ),
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
        ),
    )
    return GenericTransitionSystem(initState, alphabet, "P2", ctx)
}

fun makeTS3() : TransitionSystem {
    val ctx = Context()
    val initState = State(setOf())
    val alphabet = setOf(
        SymbolicAction(
            ActionSignature("Print", listOf("iVal")),
            ctx.mkTrue(),
            setOf(),
            setOf({ _,act -> println("Print: " + act.lookupInt("iVal")) })
        ),
    )
    return GenericTransitionSystem(initState, alphabet, "PrintProc", ctx)
}

fun main(args : Array<String>) {
    //val prog = Program(setOf(makeTS1(), makeTS2()))
    val prog = Program(setOf(makeTS1(), makeTS2(), makeTS3()))
    prog.run()
}
