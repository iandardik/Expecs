package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
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

class GenericTransitionSystem(
    initState : State,
    private val alphabet : Set<SymbolicAction>,
    private val name : String,
    private val ctx : Context
) : TransitionSystem {
    private var state = initState

    override fun getName() = name
    override fun getContext() = ctx
    override fun alphabet() = alphabet

    override fun currentState() : BoolExpr {
        return state.toExpr(ctx)
    }

    /**
     * Adds an implicit frame condition for variables to remain the same if they are not mentioned in <updates>
     */
    override fun transit(concAct : ConcreteAction) {
        val symAct = correspondingSymbolicAction(concAct)
        val explicitlyUpdatedAssignments = symAct.varUpdates
            .map { update -> update.updateAssignment(state,concAct) }
            .toSet()
        val explicitlyUpdatedVarNames = explicitlyUpdatedAssignments.map { it.varName() }.toSet()
        val frameUpdateAssignments = state.assignments
            .filter { !explicitlyUpdatedVarNames.contains(it.varName()) }
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