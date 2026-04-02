package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import exspecs.tools.assert

class State(
    val assignments : Map<Variable,Value>
) {
    fun toZ3Expr(ctx : Context) : BoolExpr {
        val equalExprs = assignments.map { (k,v) -> ctx.mkEq(k.toZ3Expr(ctx), v.toZ3Expr(ctx)) }
        return ctx.mkAnd(*equalExprs.toTypedArray())
    }

    fun lookup(variable : Variable) : Value {
        assert(variable in assignments, "State.lookup($variable): not in this state!")
        return assignments[variable]!!
    }

    override fun toString(): String {
        return assignments
            .map { (k,v) -> "  $k = $v" }
            .joinToString("\n")
    }
}

fun emptyState() : State {
    return State(emptyMap())
}