package exspecs

import com.microsoft.z3.BoolExpr

/**
 * Represents a symbolic action for a given transition system / proc.
 * This class is particular to a transition system / proc because it dictates when it's enabled (via the guard) and how
 * the transition system transits to a new state (via the var updates).
 */
data class SymbolicAction(
    val signature : ActionSignature,
    val guard : BoolExpr,
    val varUpdates : Set<StateVarUpdate>,
) {}
