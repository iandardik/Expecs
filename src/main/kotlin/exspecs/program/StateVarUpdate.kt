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
    private val varName : VarName,
    private val updateExpr : UpdateExpr<Int>
) : StateVarUpdate {
    override fun updateAssignment(state : State, act : ConcreteAction) : VarAssignment {
        return IntVarAssignment(varName, updateExpr.eval(state, act))
    }
    override fun toString(): String {
        return "$varName := $updateExpr"
    }
}

class StringStateVarUpdate(
    private val varName : VarName,
    private val updateExpr : UpdateExpr<String>
) : StateVarUpdate {
    override fun updateAssignment(state : State, act : ConcreteAction) : VarAssignment {
        return StringVarAssignment(varName, updateExpr.eval(state, act))
    }
    override fun toString(): String {
        return "$varName := $updateExpr"
    }
}
