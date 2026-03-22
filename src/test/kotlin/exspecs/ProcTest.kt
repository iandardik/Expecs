package exspecs

import com.microsoft.z3.*
import org.testng.annotations.Test

class ProcTest {

    @Test
    fun testSyncSize1() {
        for (i in 0..100) {
            runProcs(makeProcs1())
        }
    }

    @Test
    fun testSyncSize1NoArgConstraint() {
        // Unfortunately this may run forever--need to update the test
        /*
        for (i in 0..100) {
            runProcs(makeProcs1NoArgConstraint())
        }
         */
    }

    @Test
    fun testSyncSize2SameGuards() {
        for (i in 0..100) {
            runProcs(makeProcs2())
        }
    }

    @Test
    fun testSyncSize2DifferentGuards() {
        for (i in 0..100) {
            runProcs(makeProcs3())
        }
    }

    private fun makeProcs1NoArgConstraint() : List<Proc> {
        val context = Context()
        val enabledExpr = context.mkLe(context.mkIntConst("i"), context.mkInt(10))
        val actIChan = createActionChannel(1)
        val actI = SymAction("I", listOf("inc"), enabledExpr, setOf(), actIChan)
        val p1 = Proc("p1", OneIntVarTS(actI))
        return listOf(p1)
    }

    private fun makeProcs1() : List<Proc> {
        val context = Context()
        val enabledExpr = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
        val actIChan = createActionChannel(1)
        val actI = SymAction("I", listOf("inc"), enabledExpr, setOf(), actIChan)
        val p1 = Proc("p1", OneIntVarTS(actI))
        return listOf(p1)
    }

    private fun makeProcs2() : List<Proc>  {
        val context = Context()
        val enabledExpr = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
        val actIChan = createActionChannel(2)
        val actI1 = SymAction("I", listOf("inc"), enabledExpr, setOf(), actIChan)
        val p1 = Proc("p1", OneIntVarTS(actI1))
        val actI2 = SymAction("I", listOf("inc"), enabledExpr, setOf(), actIChan)
        val p2 = Proc("p2", OneIntVarTS(actI2))
        return listOf(p1,p2)
    }

    private fun makeProcs3() : List<Proc>  {
        val context = Context()
        val actIChan = createActionChannel(2)

        val enabledExpr1 = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(1)))
        val actI1 = SymAction("I", listOf("inc"), enabledExpr1, setOf(), actIChan)
        val p1 = Proc("p1", OneIntVarTS(actI1))

        val enabledExpr2 = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkLt(context.mkIntConst("inc"), context.mkInt(3)))
        val actI2 = SymAction("I", listOf("inc"), enabledExpr2, setOf(), actIChan)
        val p2 = Proc("p2", OneIntVarTS(actI2))

        return listOf(p1,p2)
    }

    private fun runProcs(procs : List<Proc>) {
        val threads = procs.map { Thread(it) }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}

class OneIntVarTS(
    private val incAct : SymAction,
) : TransitionSystem {
    private var i = 1
    override fun allActions(): Set<SymAction> {
        return setOf(incAct)
    }

    override fun enabledActions(ctx : Context): Set<SymAction> {
        val enabled = ctx.mkAnd(incAct.getEnabledExpr().translate(ctx), curStateFormula(ctx))
        val solver = ctx.mkSolver()
        solver.add(enabled)
        return if (solver.check() == Status.SATISFIABLE) {
            setOf(incAct)
        } else {
            emptySet()
        }
    }

    override fun transit(act : ConcreteAction) {
        val inc = act.lookupInt("inc")
        i += inc
    }

    private fun curStateFormula(ctx : Context) : BoolExpr {
        return ctx.mkEq(ctx.mkIntConst("i"), ctx.mkInt(i))
    }

    override fun toString(): String {
        return "i: $i"
    }
}
