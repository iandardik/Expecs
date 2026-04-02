package exspecs.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import exspecs.tools.mkStringConst

val boolType = BoolType()
val intType = IntType()
val stringType = StringType()
val baseTypes = listOf(boolType, intType, stringType)

fun parseType(strType : String) : Type {
    baseTypes.forEach { baseType ->
        if (strType == baseType.toString()) {
            return baseType
        }
    }
    return InvalidType(strType)
}

interface Type {
    fun toZ3Expr(variable : Variable, ctx : Context) : Expr<*>
    fun toZ3Expr(value : Value, ctx : Context) : Expr<*>
    fun fromZ3Expr(expr : Expr<*>) : Any
    fun isOfType(obj : Any) : Boolean
}

class InvalidType : Type {
    constructor(name : String) {
        throw RuntimeException("Invalid type: $name")
    }
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        throw RuntimeException("Invalid type")
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        throw RuntimeException("Invalid type")
    }

    override fun fromZ3Expr(expr: Expr<*>): Any {
        throw RuntimeException("Invalid type")
    }

    override fun isOfType(obj: Any): Boolean {
        throw RuntimeException("Invalid type")
    }

    override fun toString(): String {
        throw RuntimeException("Invalid type")
    }

    override fun equals(other: Any?): Boolean {
        return other is InvalidType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class BoolType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkBoolConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkBool(value.value as Boolean)
    }

    override fun fromZ3Expr(expr: Expr<*>): Any {
        return expr.string.lowercase() == "true"
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is Boolean
    }

    override fun toString(): String {
        return "Bool"
    }

    override fun equals(other: Any?): Boolean {
        return other is BoolType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class IntType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkIntConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkInt(value.value as Int)
    }

    override fun fromZ3Expr(expr: Expr<*>): Any {
        return Integer.parseInt(expr.toString())
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is Int
    }

    override fun toString(): String {
        return "Int"
    }

    override fun equals(other: Any?): Boolean {
        return other is IntType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}

class StringType : Type {
    override fun toZ3Expr(variable: Variable, ctx: Context): Expr<*> {
        return ctx.mkStringConst(variable.name)
    }

    override fun toZ3Expr(value: Value, ctx: Context): Expr<*> {
        return ctx.mkString(value.value as String)
    }

    override fun fromZ3Expr(expr: Expr<*>): Any {
        return expr.string
    }

    override fun isOfType(obj: Any): Boolean {
        return obj is String
    }

    override fun toString(): String {
        return "String"
    }

    override fun equals(other: Any?): Boolean {
        return other is StringType
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}
