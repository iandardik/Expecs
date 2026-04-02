package exspecs.program

import exspecs.tools.assert

interface ProgramExpr {
    fun eval(state : State, act : ConcreteAction) : Value
}

class PlusProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValue = lhsExpr.eval(state,act)
        val rhsValue = rhsExpr.eval(state,act)
        assert(lhsValue.type == rhsValue.type, "Expected matching types in: $lhsExpr + $rhsExpr")
        return when (lhsValue.type) {
            is IntType -> Value(lhsValue.value as Int + rhsValue.value as Int, intType)
            is StringType -> Value(lhsValue.value as String + rhsValue.value as String, stringType)
            else -> throw RuntimeException("Cannot add type ${lhsValue.type} in expression: $lhsExpr + $rhsExpr")
        }
    }
}

class EqProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValue = lhsExpr.eval(state,act)
        val rhsValue = rhsExpr.eval(state,act)
        assert(lhsValue.type == rhsValue.type, "Expected matching types in equality: $lhsExpr = $rhsExpr")
        return Value(lhsValue.value == rhsValue.value, boolType)
    }
}

class NotProgramExpr(
    private val expr : ProgramExpr
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val valuation = evalToType(boolType, expr, state, act) as Boolean
        return Value(!valuation, boolType)
    }
}

class LtProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Int
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Int
        return Value(lhsValuation < rhsValuation, boolType)
    }
}

class LeProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Int
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Int
        return Value(lhsValuation <= rhsValuation, boolType)
    }
}

class GtProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Int
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Int
        return Value(lhsValuation > rhsValuation, boolType)
    }
}

class GeProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Int
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Int
        return Value(lhsValuation >= rhsValuation, boolType)
    }
}

class AndProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Boolean
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Boolean
        return Value(lhsValuation && rhsValuation, boolType)
    }
}

class OrProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Boolean
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Boolean
        return Value(lhsValuation || rhsValuation, boolType)
    }
}

class MinusProgramExpr(
    private val lhsExpr : ProgramExpr,
    private val rhsExpr : ProgramExpr,
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        val lhsValuation = evalToType(intType, lhsExpr, state, act) as Int
        val rhsValuation = evalToType(intType, rhsExpr, state, act) as Int
        return Value(lhsValuation - rhsValuation, intType)
    }
}

class BoolLiteralProgramExpr(
    private val value : Boolean
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        return Value(value, boolType)
    }
}

class IntLiteralProgramExpr(
    private val value : Int
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        return Value(value, intType)
    }
}

class StringLiteralProgramExpr(
    private val value : String
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        return Value(value, stringType)
    }
}

class SymbolProgramExpr(
    private val variable : Variable
) : ProgramExpr {
    override fun eval(state: State, act: ConcreteAction): Value {
        if (act.hasArg(variable)) {
            return act.lookup(variable)
        }
        return state.lookup(variable)
    }
}

fun evalToType(type : Type, expr : ProgramExpr, state : State, act : ConcreteAction) : Any {
    val valuation = expr.eval(state,act)
    assert(valuation.type == type, "Expected type: $type from the expr: $expr")
    return valuation.value
}