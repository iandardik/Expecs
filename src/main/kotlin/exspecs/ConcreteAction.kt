package exspecs

import com.microsoft.z3.Context
import com.microsoft.z3.Model

class ConcreteAction(
    private val symAction : SymAction,
    model : Model,
    ctx : Context,
) {
    private val valueMap : Map<String,Int> = symAction.getArgNames()
        .associateWith {
            val valExpr = model.eval(ctx.mkIntConst(it), true)
            Integer.parseInt(valExpr.toString())
        }
    fun lookupInt(arg : String) : Int {
        return valueMap[arg]!!
    }

    override fun toString() : String {
        return "ConcreteAction($symAction): " + symAction.getArgNames().joinToString(",") { "$it->" + lookupInt(it).toString() }
    }
}