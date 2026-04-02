package exspecs.program

import com.microsoft.z3.Context
import com.microsoft.z3.Model
import exspecs.tools.assert

class ConcreteAction(
    val signature : ActionSignature,
    private val argAssignments : Map<Variable,Value>
) {
    constructor(sig : ActionSignature, ctx : Context, model : Model)
        : this(sig, sig.args.associateWith { v ->
            val z3Value = model.eval(v.type.toZ3Expr(v,ctx), true)
            Value(z3Value, v.type)
        }) {}

    fun hasArg(arg : Variable) : Boolean {
        return arg in argAssignments
    }

    fun lookup(variable : Variable) : Value {
        assert(variable in argAssignments, "ConcreteAction.lookup($variable): is not assigned a value!")
        return argAssignments[variable]!!
    }

    override fun toString() : String {
        return "ConcreteAction($signature): $argAssignments"
    }
}

fun emptyConcreteAction() : ConcreteAction {
    return ConcreteAction(ActionSignature("",emptyList()), emptyMap())
}