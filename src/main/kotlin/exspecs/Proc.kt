package exspecs

import com.microsoft.z3.Status
import com.microsoft.z3.BoolExpr
import java.util.*

class Proc(
    private val transitionSystem : TransitionSystem,
    private val channelTable : Map<ActionSignature, SyncChannel<ConcreteAction,BoolExpr>>
) : Runnable {
    private val name = transitionSystem.getName()
    private val ctx = transitionSystem.getContext()

    override fun run() {
        while (true) {
            var nextAct = Optional.empty<ConcreteAction>()
            val enabledActions = transitionSystem.alphabet().filter {
                val enabled = ctx.mkAnd(it.guard, transitionSystem.currentState())
                val solver = ctx.mkSolver()
                solver.add(enabled)
                solver.check() == Status.SATISFIABLE
            }
            val cases = enabledActions.map { symAct ->
                val channel = channelTable[symAct.signature]!!
                Select.SyncCase(channel, symAct.guard) { concAct : ConcreteAction ->
                    nextAct = Optional.of(concAct)
                }
            }
            Select(*cases.toTypedArray()).run()

            println("$name cur state:\n$transitionSystem\n")

            // check for deadlocks
            if (nextAct.isEmpty) {
                println("$name: Deadlock") // TODO delete
                return
            }

            println("$name: ${nextAct.get()}\n") // TODO delete
            //Thread.sleep(1000) // TODO delete

            // transit to the next state
            transitionSystem.transit(nextAct.get())
        }
    }

    override fun toString() : String {
        return "Proc($name)"
    }
}
