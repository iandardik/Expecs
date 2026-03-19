package exspecs

import com.microsoft.z3.*
import java.lang.RuntimeException

class TS1(
    private val incAct : SymAction,
    private val context : Context
) : TransitionSystem {
    private var i = 1
    override fun enabledActions(): Set<SymAction> {
        //val enabled = incAct.getEnabledFormula().and(curStateFormula())
        //return if (enabled.sat().isPresent) {
        // TODO this is also bad
        val enabled = context.mkAnd(incAct.getEnabledFormula(),curStateFormula())
        //println("enabled formula: $enabled")
        val solver = context.mkSolver()
        solver.add(enabled)
        return if (solver.check() == Status.SATISFIABLE) {
            setOf(incAct)
        } else {
            emptySet()
        }
    }

    override fun transit(act : ConcreteAction) {
        val inc = act.lookup("inc")
        if (inc is Int) {
            i += inc
        } else {
            throw RuntimeException("TS1: inc is the wrong type!")
        }
    }

    private fun curStateFormula() : BoolExpr {
        val initState = context.mkEq(context.mkIntConst("i"), context.mkInt(i))
        //return Formula(initState, context)
        return initState
    }

    override fun toString(): String {
        return "i: $i"
    }
}

fun main(args : Array<String>) {
    val context = Context()
    //val actForm = context.mkLe(context.mkIntConst("i"), context.mkInt(10))
    val actForm = context.mkAnd(context.mkLe(context.mkIntConst("i"), context.mkInt(10)), context.mkGt(context.mkIntConst("inc"), context.mkInt(3)))
    //val enabledFormula = Formula(actForm, context)
    val inc = SymAction("I", listOf("inc"), actForm, 1, context)
    val p = Proc("p", TS1(inc, context))
    p.run()

    /*
    val ctx = Context()
    val solver : Solver = ctx.mkSolver()

    val a : BoolExpr = ctx.mkBoolConst("a")
    solver.add(a)

    println(solver.check())
    //println(solver.model)
    println(solver.model.getConstInterp(a))
    println(solver.model.getConstInterp(ctx.mkBoolConst("a")))
    println(solver.model.getConstInterp(ctx.mkBoolConst("b")))
     */
}