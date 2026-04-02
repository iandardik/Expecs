package exspecs.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr

/**
 * Represents a typed variable, including state variables and action arguments.
 */
data class Variable(
    val name : String,
    val type : Type
) {
    fun toZ3Expr(ctx : Context) : Expr<*> {
        return type.toZ3Expr(this, ctx)
    }

    override fun toString(): String {
        return "$name : $type"
    }
}