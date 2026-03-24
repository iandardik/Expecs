package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import exspecs.tools.mkStringConst

class VarName(
    val name : String
) {
    override fun toString(): String {
        return name
    }
    override fun equals(other: Any?): Boolean {
        return other is VarName && name == other.name
    }
    override fun hashCode(): Int {
        return name.hashCode()
    }
}

interface VarAssignment {
    fun varName() : VarName
    fun toExpr(ctx : Context) : BoolExpr
    fun value() : Any
}

abstract class NamedVarAssignment(
    private val varName : VarName
) : VarAssignment {
    override fun varName() : VarName = varName
    override fun equals(other: Any?): Boolean {
        return other is NamedVarAssignment && varName == other.varName
    }
    override fun hashCode(): Int {
        return varName.hashCode()
    }
}

class IntVarAssignment(
    varName : VarName,
    private val varVal : Int
) : NamedVarAssignment(varName) {
    override fun toExpr(ctx : Context) : BoolExpr {
        return ctx.mkEq(ctx.mkIntConst(varName().name), ctx.mkInt(varVal))
    }
    override fun toString(): String {
        return "${varName()} |-> $varVal"
    }
    override fun value(): Any {
        return varVal
    }
}

class StringVarAssignment(
    varName : VarName,
    private val varVal : String
) : NamedVarAssignment(varName) {
    override fun toExpr(ctx : Context) : BoolExpr {
        return ctx.mkEq(mkStringConst(varName().name,ctx), ctx.mkString(varVal))
    }
    override fun toString(): String {
        return "${varName()} |-> $varVal"
    }
    override fun value(): Any {
        return varVal
    }
}
