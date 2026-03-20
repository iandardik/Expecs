package exspecs

import com.microsoft.z3.*
import java.util.*

class EnabledFormula {
    private val act : Optional<SymAction>
    private val expr : Expr<BoolSort>

    constructor(action : SymAction) {
        act = Optional.of(action)
        expr = action.getEnabledExpr()
    }

    constructor(expression : Expr<BoolSort>) {
        act = Optional.empty()
        expr = expression
    }

    private constructor(action : Optional<SymAction>, expression : Expr<BoolSort>) {
        act = action
        expr = expression
    }

    fun and(other : EnabledFormula, ctx : Context) : EnabledFormula {
        if (act.isEmpty) {
            return other
        }
        if (other.act.isEmpty) {
            return this
        }
        // TODO ensure that the SymAct's for this and other are equal
        return EnabledFormula(act, ctx.mkAnd(expr.translate(ctx), other.expr.translate(ctx)))
    }

    /**
     * Returns a model (<ConcreteAction>) if the formula is satisfiable and an empty <Optional> otherwise.
     */
    fun sat(ctx : Context) : Optional<ConcreteAction> {
        val solver = ctx.mkSolver()
        solver.add(expr.translate(ctx))
        return if (solver.check() == Status.SATISFIABLE) {
            Optional.of(ConcreteAction(act.get(), solver.model, ctx))
        } else {
            Optional.empty()
        }
    }
}
fun tt(ctx : Context) : EnabledFormula {
    return EnabledFormula(ctx.mkTrue())
}