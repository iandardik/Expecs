package exspecs.program

import com.microsoft.z3.Context
import com.microsoft.z3.Expr
import exspecs.tools.assert

data class Value(
    val value : Any,
    val type : Type
) {
    constructor(z3Value : Expr<*>, ty : Type) : this(ty.fromZ3Expr(z3Value), ty) {}

    init {
        assert(type.isOfType(value), "Value constructed with mismatched value and type: $value : $type")
    }

    fun toZ3Expr(ctx : Context) : Expr<*> {
        return type.toZ3Expr(this, ctx)
    }

    override fun toString(): String {
        return value.toString()
    }
}