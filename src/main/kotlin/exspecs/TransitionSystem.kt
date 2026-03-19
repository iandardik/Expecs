package exspecs

interface TransitionSystem {
    fun enabledActions() : Set<SymAction>
    fun transit(act : ConcreteAction)
}