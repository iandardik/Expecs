package exspecs.tools

import com.microsoft.z3.*

fun Context.mkStringConst(name : String) : Expr<SeqSort<CharSort>> {
    return this.mkConst(this.mkSymbol(name), this.mkStringSort())
}
