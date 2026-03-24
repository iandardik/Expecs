package exspecs.program

import com.microsoft.z3.*
import org.testng.annotations.Test

class ProgTest {

    @Test
    fun testSyncSize1() {
        for (i in 0..100) {
            Program(setOf(makeTS1())).run()
        }
    }

    @Test
    fun testSyncSize2() {
        for (i in 0..100) {
            Program(setOf(makeTS1(), makeTS2())).run()
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
}
