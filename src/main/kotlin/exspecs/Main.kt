package exspecs

import com.microsoft.z3.*

class TS1(
    private val incAct : SymAction,
) : TransitionSystem {
    private var i = 1
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

fun makeProcs1() : List<Proc> {
    val context = Context()
    //val enabledExpr = context.mkLe(context.mkIntConst("i"), context.mkInt(10))
    val enabledExpr = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
    val actIChan = createActionChannel(1)
    val actI = SymAction("I", listOf("inc"), enabledExpr, actIChan)
    val p1 = Proc("p1", TS1(actI))
    return listOf(p1)
}

fun makeProcs2() : List<Proc>  {
    val context = Context()
    //val enabledExpr = context.mkLe(context.mkIntConst("i"), context.mkInt(10))
    val enabledExpr = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
    val actIChan = createActionChannel(2)
    val actI1 = SymAction("I", listOf("inc"), enabledExpr, actIChan)
    val p1 = Proc("p1", TS1(actI1))
    val actI2 = SymAction("I", listOf("inc"), enabledExpr, actIChan)
    val p2 = Proc("p2", TS1(actI2))
    return listOf(p1,p2)
}

fun runProcs(procs : List<Proc>) {
    val threads = procs.map { Thread(it) }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

fun main(args : Array<String>) {
    for (i in 0..100) {
        runProcs(makeProcs1())
    }
    for (i in 0..100) {
        runProcs(makeProcs2())
    }
}
