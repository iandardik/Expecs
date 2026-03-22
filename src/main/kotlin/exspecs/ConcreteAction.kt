package exspecs

import com.microsoft.z3.*

class ConcreteAction {
    private val valueMap : Map<String,Int>
    val symAction : SymAction
    constructor(symAction : SymAction, model : Model, ctx : Context) {
        this.symAction = symAction
        valueMap = symAction.getArgNames()
            .associateWith {
                val valExpr = model.eval(ctx.mkIntConst(it), true)
                Integer.parseInt(valExpr.toString())
            }
    }
    constructor(other : ConcreteAction, newSymAct : SymAction) {
        symAction = newSymAct
        valueMap = other.valueMap
    }

    fun hasVar(arg : VarName) : Boolean {
        return valueMap.containsKey(arg.name)
    }

    fun lookupInt(arg : String) : Int {
        return valueMap[arg]!!
    }

    override fun toString() : String {
        return "ConcreteAction($symAction): " + symAction.getArgNames().joinToString(",") { "$it->" + lookupInt(it).toString() }
    }
}