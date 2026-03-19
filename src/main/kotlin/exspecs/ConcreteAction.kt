package exspecs

import com.microsoft.z3.Context
import com.microsoft.z3.Model

class ConcreteAction(
    private val symAction : SymAction,
    private val model : Model,
    private val context : Context,
    //private val argMap : Map<String, Int>
) {
    fun lookup(arg : String) : Int {
        //return argMap[arg]!!
        val intVal = model.eval(context.mkIntConst(arg), true)
        return Integer.parseInt(intVal.toString())
    }

    override fun toString(): String {
        //return "ConcreteAction($symAction, $argMap)"
        return "ConcreteAction($symAction): " + symAction.getArgNames().joinToString(",") { "$it->" + lookup(it).toString() }
    }
}