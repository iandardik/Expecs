package exspecs

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

class SymAction(
    private val name : String,
    private val argNames : List<String>,
    private val enabledExpr : BoolExpr,
    private val syncSize : Int,
) {
    private val ctx = Context()
    val channel = SyncChannel(syncSize) { constraints ->
        val conj = constraints.fold(tt(ctx)) { acc, f ->
            acc.and(f, ctx)
        }
        conj.sat(ctx)
    }

    fun getName() = name
    fun getArgNames() = argNames
    fun getEnabledExpr() = enabledExpr

    fun toEnabledFormula() : Formula {
        return Formula(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is SymAction && this.name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "SymAction(syncSize=$syncSize): $name(" + argNames.joinToString(",") + ")"
    }
}
