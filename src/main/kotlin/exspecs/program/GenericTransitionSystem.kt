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

    override fun currentStateToZ3Expr() : BoolExpr {
        return state.toZ3Expr(ctx)
    }

    /**
     * Adds an implicit frame condition for variables to remain the same if they are not mentioned in <updates>
     */
    override fun transit(concAct : ConcreteAction) {
        val symAct = correspondingSymbolicAction(concAct)
        symAct.sideEffect.ifPresent { state = it.invoke(state,concAct) }
        val updatedAssignments = state.assignments.map { (k,v) ->
            if (k in symAct.varUpdates) {
                Pair(k, symAct.varUpdates[k]!!.eval(state,concAct))
            } else {
                Pair(k,v)
            }
        }.associate { it }
        state = State(updatedAssignments)
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