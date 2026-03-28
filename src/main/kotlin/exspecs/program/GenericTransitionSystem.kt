package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context

class GenericTransitionSystem(
    initState : State,
    private val alphabet : Set<SymbolicAction>,
    private val name : String,
    private val ctx : Context,
    private val selfTerminate : Boolean = true,
) : TransitionSystem {
    private var state = initState

    override fun getName() = name
    override fun getContext() = ctx
    override fun alphabet() = alphabet
    override fun selfTerminate() = selfTerminate

    override fun currentState() : BoolExpr {
        return state.toExpr(ctx)
    }

    /**
     * Adds an implicit frame condition for variables to remain the same if they are not mentioned in <updates>
     */
    override fun transit(concAct : ConcreteAction) {
        val symAct = correspondingSymbolicAction(concAct)
        symAct.sideEffects.forEach { state = it.invoke(state,concAct) }
        val explicitlyUpdatedAssignments = symAct.varUpdates
            .map { update -> update.updateAssignment(state,concAct) }
            .toSet()
        val explicitlyUpdatedVarNames = explicitlyUpdatedAssignments.map { it.getVariable() }.toSet()
        val frameUpdateAssignments = state.assignments
            .filter { !explicitlyUpdatedVarNames.contains(it.getVariable()) }
            .toSet()
        val allUpdatedAssignments = explicitlyUpdatedAssignments union frameUpdateAssignments
        state = State(allUpdatedAssignments)
    }

    private fun correspondingSymbolicAction(concAct : ConcreteAction) : SymbolicAction {
        val symActs = alphabet.filter { it.signature == concAct.signature }
        exspecs.tools.assert(
            symActs.size == 1,
            "correspondingSymbolicAction() expected 1 symAct but found: ${symActs.size}"
        )
        return symActs.first()
    }

    override fun toString(): String {
        // TODO we can probably do better than this
        return state.toString()
        //return state.toString() + "\nupdates:\n" + actions.map { it.updates.joinToString(", ") }
    }
}