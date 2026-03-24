package exspecs.program

interface UpdateExpr<T> {
    fun eval(state : State, act : ConcreteAction) : T
}

class EqUpdateExpr<T>(
    private val lhs : UpdateExpr<T>,
    private val rhs : UpdateExpr<T>,
) : UpdateExpr<Boolean> {
    override fun eval(state : State, act : ConcreteAction) : Boolean {
        return lhs.eval(state, act) == rhs.eval(state, act)
    }
}

class IntUpdateExpr(
    private val intValue : Int
) : UpdateExpr<Int> {
    override fun eval(state : State, act : ConcreteAction) : Int {
        return intValue
    }
    override fun toString(): String {
        return "$intValue"
    }
}

class IntVarUpdateExpr(
    private val varName : VarName
) : UpdateExpr<Int> {
    override fun eval(state : State, act : ConcreteAction) : Int {
        // action parameters are at a tighter scope than the state variables
        if (act.hasVar(varName)) {
            return act.lookupInt(varName.name) // TODO clean up the VarName stuff
        }
        return state.lookupIntVal(varName)
    }
    override fun toString(): String {
        return "$varName"
    }
}

class PlusIntUpdateExpr(
    private val lhs : UpdateExpr<Int>,
    private val rhs : UpdateExpr<Int>,
) : UpdateExpr<Int> {
    override fun eval(state : State, act : ConcreteAction) : Int {
        return lhs.eval(state, act) + rhs.eval(state, act)
    }
    override fun toString(): String {
        return "($lhs + $rhs)"
    }
}

class LtIntUpdateExpr(
    private val lhs : UpdateExpr<Int>,
    private val rhs : UpdateExpr<Int>,
) : UpdateExpr<Boolean> {
    override fun eval(state : State, act : ConcreteAction) : Boolean {
        return lhs.eval(state, act) < rhs.eval(state, act)
    }
}

class LeIntUpdateExpr(
    private val lhs : UpdateExpr<Int>,
    private val rhs : UpdateExpr<Int>,
) : UpdateExpr<Boolean> {
    override fun eval(state : State, act : ConcreteAction) : Boolean {
        return lhs.eval(state, act) <= rhs.eval(state, act)
    }
}

class StrUpdateExpr(
    private val strValue : String
) : UpdateExpr<String> {
    override fun eval(state : State, act : ConcreteAction) : String {
        return strValue
    }
}

class StrVarUpdateExpr(
    private val varName : VarName
) : UpdateExpr<String> {
    override fun eval(state : State, act : ConcreteAction) : String {
        return state.lookupStrVal(varName)
    }
}
