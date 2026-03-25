package exspecs.program

import com.microsoft.z3.*
import org.testng.annotations.Test

class ProgTest {
    /*

    @Test
    fun testSyncSize1() {
        for (i in 0..100) {
            Program(setOf(makeTS1())).testRun()
        }
    }

    @Test
    fun testSyncSize2Test1() {
        for (i in 0..100) {
            Program(setOf(makeTS1(), makeTS2())).testRun()
        }
    }

    @Test
    fun testSyncSize2Test2() {
        for (i in 0..100) {
            Program(setOf(makeTS2(), makeTS3())).testRun()
        }
    }

    private fun makeTS1() : TransitionSystem {
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

    private fun makeTS2() : TransitionSystem {
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

    fun makeTS3() : TransitionSystem {
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
                ActionSignature("Println2", listOf("msg")), // don't actually use the library function here
                ctx.mkAnd(
                    ctx.mkEq(ctx.mkIntConst("msg"),ctx.mkIntConst("i")),
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
        return GenericTransitionSystem(initState, alphabet, "P3", ctx)
    }
     */
}
