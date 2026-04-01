package exspecs.program

/**
 * Represents the expression in an update: var := <expr>.
 * The value may depend on the current state of the state vars, so updateAssignment() accepts a State object.
 * The value may also depend on the params to the action, so updateAssignment() also accepts a ConcreteAction object.
 */
interface StateVarUpdate {
    fun updateAssignment(state : State, act : ConcreteAction) : VarAssignment
}

class IntStateVarUpdate(
    private val variable : Variable,
    private val updateExpr : UpdateExpr<Int>
) : StateVarUpdate {
    override fun updateAssignment(state : State, act : ConcreteAction) : VarAssignment {
        return IntVarAssignment(variable, updateExpr.eval(state, act))
    }
    override fun toString(): String {
        return "${variable.name} := $updateExpr"
    }
}

class BoolStateVarUpdate(
    private val variable : Variable,
    private val updateExpr : UpdateExpr<Boolean>
) : StateVarUpdate {
    override fun updateAssignment(state : State, act : ConcreteAction) : VarAssignment {
        return BoolVarAssignment(variable, updateExpr.eval(state, act))
    }
    override fun toString(): String {
        return "${variable.name} := $updateExpr"
    }
}

class StringStateVarUpdate(
    private val variable : Variable,
    private val updateExpr : UpdateExpr<String>
) : StateVarUpdate {
    override fun updateAssignment(state : State, act : ConcreteAction) : VarAssignment {
        return StringVarAssignment(variable, updateExpr.eval(state, act))
    }
    override fun toString(): String {
        return "${variable.name} := $updateExpr"
    }
}
