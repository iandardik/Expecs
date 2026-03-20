package exspecs

import com.microsoft.z3.Context

interface TransitionSystem {
    fun enabledActions(ctx : Context) : Set<SymAction>
    fun transit(act : ConcreteAction)
}