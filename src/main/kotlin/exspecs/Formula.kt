package exspecs

import com.microsoft.z3.*
import java.util.*

// TODO a better name is probably ActionFormula or something like that
class Formula(
    private val act : SymAction?, // TODO make this cleaner
    private val context : Context,
    private val expr : BoolExpr = act!!.getEnabledFormula()
) {
    val solver = context.mkSolver()

    fun and(other : Formula) : Formula {
        if (act == null) {
            return other
        }
        return Formula(act, context, context.mkAnd(expr, other.expr))
    }

    /**
     * Returns a model (<ConcreteAction>) if the formula is satisfiable and an empty <Optional> otherwise.
     */
    fun sat() : Optional<ConcreteAction> {
        solver.add(expr)
        return if (solver.check() == Status.SATISFIABLE) {
            /*
            // TODO not hardcode this to ints
            val names = solver.model.constDecls.map { it.name.toString() }
            // TODO this is disgusting
            val valMap = names.associateWith { arg -> Integer.parseInt(solver.model.getConstInterp(context.mkIntConst(arg)).toString()) }
            Optional.of(ConcreteAction(act!!, valMap))
             */
            Optional.of(ConcreteAction(act!!, solver.model, context))
        } else {
            Optional.empty()
        }
    }
}
fun tt() : Formula {
    val ctx = Context()
    return Formula(null, ctx, ctx.mkTrue())
}