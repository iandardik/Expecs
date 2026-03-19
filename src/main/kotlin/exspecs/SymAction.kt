package exspecs

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

class SymAction(
    private val name : String,
    private val argNames : List<String>,
    private val enabledFormula : BoolExpr,
    private val syncSize : Int,
    private val context : Context,
) {
    val channel = SyncChannel(syncSize) { constraints ->
        val conj = constraints.fold(tt()) { acc, f ->
            acc.and(f)
        }
        conj.sat()
    }

    fun getName() = name
    fun getArgNames() = argNames
    fun getEnabledFormula() = enabledFormula

    fun toEnabledFormula() : Formula {
        return Formula(this, context, enabledFormula)
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