package exspecs

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

/**
 * Transition systems do not worry about channels or any kind of communication--they simply deal with their own internal
 * workings. Procs, on the other hand, are what deal with communication and synchronization.
 */
interface TransitionSystem {
    fun alphabet() : Set<SymbolicAction>

    fun currentState() : BoolExpr

    fun transit(act : ConcreteAction)

    fun getName() : String

    /**
     * Exactly one Context should be used, and should be available here for public use. Using just one Context is
     * important for thread safety, see: https://stackoverflow.com/questions/25542200/multi-threaded-z3
     */
    fun getContext() : Context
}