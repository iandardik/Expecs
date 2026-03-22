package exspecs

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import java.lang.RuntimeException

class State(
    val assignments : Set<VarAssignment>
) {
    fun toExpr(ctx : Context) : BoolExpr {
        return assignments.fold(ctx.mkTrue()) { acc,assg -> ctx.mkAnd(acc,assg.toExpr(ctx)) }
    }

    fun lookupIntVal(varName : VarName) : Int {
        val matching = assignments.filter { it.varName() == varName }
        if (matching.size == 1) {
            return matching.first().value() as Int
        } else {
            throw RuntimeException("Expected one var name ($varName) in the state:\n$this")
        }
    }

    fun lookupStrVal(varName : VarName) : String {
        val matching = assignments.filter { it.varName() == varName }
        if (matching.size == 1) {
            return matching.first().value() as String
        } else {
            throw RuntimeException("Expected one var name ($varName) in the state:\n$this")
        }
    }

    override fun toString(): String {
        return assignments.joinToString("\n") { it.toString() }
    }
}

class GenericTransitionSystem : TransitionSystem {
    private val actions : Set<SymAction>
    private var state : State

    constructor(acts : Set<SymAction>, initState : State) {
        actions = acts
        state = initState
    }

    // TODO add constructor that accepts builders

    override fun allActions() = actions

    override fun enabledActions(ctx : Context): Set<SymAction> {
        return actions.filter {
            val enabled = ctx.mkAnd(it.getEnabledExpr().translate(ctx), curStateFormula(ctx))
            val solver = ctx.mkSolver()
            solver.add(enabled)
            solver.check() == Status.SATISFIABLE
        }.toSet()
    }

    /**
     * Adds an implicit frame condition for variables to remain the same if they are not mentioned in <updates>
     */
    override fun transit(act : ConcreteAction) {
        val explicitlyUpdatedAssignments = act.symAction.updates
            .map { update -> update.updateAssignment(state,act) }
            .toSet()
        val explicitlyUpdatedVarNames = explicitlyUpdatedAssignments.map { it.varName() }.toSet()
        val frameUpdateAssignments = state.assignments
            .filter { !explicitlyUpdatedVarNames.contains(it.varName()) }
            .toSet()
        val allUpdatedAssignments = explicitlyUpdatedAssignments union frameUpdateAssignments
        state = State(allUpdatedAssignments)
    }

    private fun curStateFormula(ctx : Context) : BoolExpr {
        return state.toExpr(ctx)
    }

    // TODO we can probably do better than this
    override fun toString(): String {
        return state.toString()
        //return state.toString() + "\nupdates:\n" + actions.map { it.updates.joinToString(", ") }
    }
}