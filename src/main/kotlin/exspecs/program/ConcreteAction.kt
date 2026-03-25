package exspecs.program

import exspecs.tools.assert

class ConcreteAction(
    val signature : ActionSignature,
    private val argValues : Set<VarAssignment>,
) {
    init {
        val variablesInSignature = signature.args.toSet()
        val variablesAssigned = argValues.map { it.getVariable() }.toSet()
        assert(variablesInSignature == variablesAssigned,
            "ConcreteAction: expected sig and assigned variables to be identical")
    }

    fun hasArg(arg : Variable) : Boolean {
        return argValues.any { it.getVariable() == arg }
    }

    fun lookup(arg : Variable) : Any {
        val argMatches = argValues.filter { it.getVariable() == arg }
        assert(argMatches.size == 1, "ConcreteAction: expected one assignment to variable: $arg")
        return argMatches.first().getValue()
    }

    override fun toString() : String {
        return "ConcreteAction($signature): $argValues"
    }
}