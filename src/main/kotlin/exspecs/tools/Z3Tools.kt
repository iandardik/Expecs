package exspecs.tools

import com.microsoft.z3.*

fun mkStringConst(name : String, ctx : Context) : Expr<SeqSort<CharSort>> {
    return ctx.mkConst(ctx.mkSymbol(name), ctx.mkStringSort());
}
