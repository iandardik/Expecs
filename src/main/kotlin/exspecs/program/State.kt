package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import exspecs.tools.assert

class State(
    val assignments : Set<VarAssignment>
) {
    fun toExpr(ctx : Context) : BoolExpr {
        return assignments.fold(ctx.mkTrue()) { acc,assg -> ctx.mkAnd(acc,assg.toExpr(ctx)) }
    }

    fun lookup(arg : Variable) : Any {
        val argMatches = assignments.filter { it.getVariable() == arg }
        assert(argMatches.size == 1, "State: expected one assignment to variable: $arg, found ${argMatches.size}\n$assignments")
        return argMatches.first().getValue()
    }

    override fun toString(): String {
        return assignments.joinToString("\n") { it.toString() }
    }
}
