package exspecs

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

class SymAction(
    private val name : String,
    private val argNames : List<String>,
    private val enabledExpr : BoolExpr,
    val updates : Set<StateVarUpdate>,
    val channel : SyncChannel<ConcreteAction,EnabledFormula>
) {

    fun getName() = name
    fun getArgNames() = argNames
    fun getEnabledExpr() = enabledExpr

    fun toEnabledFormula() : EnabledFormula {
        return EnabledFormula(this)
    }

    fun signature() : String {
        return "SymAction: $name(" + argNames.joinToString(",") + ")"
    }

    override fun toString(): String {
        return signature()
    }
}

fun createActionChannel(syncSize : Int) : SyncChannel<ConcreteAction,EnabledFormula> {
    val ctx = Context()
    return SyncChannel(syncSize) { constraints ->
        val conj = constraints.fold(tt(ctx)) { acc, f ->
            acc.and(f, ctx)
        }
        conj.sat(ctx)
    }
}