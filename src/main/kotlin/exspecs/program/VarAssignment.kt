package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Model
import exspecs.tools.mkStringConst

fun createVarAssignment(variable : Variable, ctx : Context, model : Model) : VarAssignment {
    return when (variable.type) {
        "Int" -> {
            val valExpr = model.eval(ctx.mkIntConst(variable.name), true)
            val intVal = Integer.parseInt(valExpr.toString())
            IntVarAssignment(variable, intVal)
        }
        "String" -> {
            val valExpr = model.eval(ctx.mkStringConst(variable.name), true)
            val intVal = Integer.parseInt(valExpr.toString())
            IntVarAssignment(variable, intVal)
        }
        else -> throw RuntimeException("createVarAssignment(): Unsupported type: ${variable.type}")
    }
}

interface VarAssignment {
    fun getVariable() : Variable
    fun toExpr(ctx : Context) : BoolExpr
    fun getValue() : Any
}

abstract class VarAssignmentBase(
    private val variable : Variable
) : VarAssignment {
    override fun getVariable() = variable
    override fun equals(other: Any?): Boolean {
        return other is VarAssignmentBase && variable == other.variable
    }
    override fun hashCode(): Int {
        return variable.hashCode()
    }
}

class IntVarAssignment(
    variable : Variable,
    private val value : Int
) : VarAssignmentBase(variable) {
    init {
        exspecs.tools.assert(variable.type == "Int", "IntVarAssignment expected variable of type Int")
    }
    override fun toExpr(ctx : Context) : BoolExpr {
        return ctx.mkEq(ctx.mkIntConst(getVariable().name), ctx.mkInt(value))
    }
    override fun toString(): String {
        return "${getVariable()} |-> $value"
    }
    override fun getValue() = value
}

class StringVarAssignment(
    variable : Variable,
    private val value : String
) : VarAssignmentBase(variable) {
    init {
        exspecs.tools.assert(variable.type == "String", "StringVarAssignment expected variable of type String")
    }
    override fun toExpr(ctx : Context) : BoolExpr {
        return ctx.mkEq(ctx.mkStringConst(getVariable().name), ctx.mkString(value))
    }
    override fun toString(): String {
        return "${getVariable()} |-> $value"
    }
    override fun getValue() = value
}
