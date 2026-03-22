package exspecs

import com.microsoft.z3.Context
import java.util.*

class Proc(
    private val name : String,
    private val sys : TransitionSystem
) : Runnable {
    private val ctx = Context()
    override fun run() {
        while (true) {
            var nextAct = Optional.empty<ConcreteAction>()
            val cases = sys.enabledActions(ctx).map { symAct ->
                Select.SyncCase(symAct.channel, symAct.toEnabledFormula()) { concAct : ConcreteAction ->
                    nextAct = Optional.of(concAct)
                }
            }
            Select(*cases.toTypedArray()).run()

            println("$name cur state:\n$sys\n")

            // check for deadlocks
            if (nextAct.isEmpty) {
                println("$name: Deadlock") // TODO delete
                return
            }

            // we do this because the concrete action is created with the SymAction from the winning thread, which may
            // be a different thread and hence have a different SymAction (with a different transition system).
            // TODO make this cleaner
            val myNextSymAct = sys.allActions().filter { it.getName() == nextAct.get().symAction.getName() }.first()
            val myNextAct = ConcreteAction(nextAct.get(), myNextSymAct)

            println("$name: ${myNextAct}\n") // TODO delete
            //Thread.sleep(1000) // TODO delete

            // transit to the next state
            sys.transit(myNextAct)
        }
    }

    override fun toString(): String {
        return "Proc($name)"
    }
}
