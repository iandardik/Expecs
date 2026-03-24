package exspecs.tools

import java.lang.RuntimeException

fun assert(expr : Boolean, msg : String = "") {
    if (!expr) {
        throw RuntimeException(msg)
    }
}