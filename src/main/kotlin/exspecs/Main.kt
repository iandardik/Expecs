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

fun trial1() {
    val context = Context()
    //val actForm = context.mkLe(context.mkIntConst("i"), context.mkInt(10))
    val actForm = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
    //val enabledFormula = Formula(actForm, context)
    val inc = SymAction("I", listOf("inc"), actForm, 1)
    val p1 = Proc("p1", TS1(inc))
    val procs = listOf(p1)
    val threads = procs.map { Thread(it) }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

fun trial2() {
    val context = Context()
    //val actForm = context.mkLe(context.mkIntConst("i"), context.mkInt(10))
    val actForm = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
    //val enabledFormula = Formula(actForm, context)
    val inc = SymAction("I", listOf("inc"), actForm, 2)
    val p1 = Proc("p1", TS1(inc))
    val p2 = Proc("p2", TS1(inc))
    val procs = listOf(p1,p2)
    val threads = procs.map { Thread(it) }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
}

fun main(args : Array<String>) {
    for (i in 0..100) {
        trial1()
    }
    for (i in 0..100) {
        trial2()
    }
}
