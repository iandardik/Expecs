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

            println("$name cur state: $sys")

            // check for deadlocks
            if (nextAct.isEmpty) {
                println("$name: Deadlock") // TODO delete
                return
            }
            println("$name: ${nextAct.get()}") // TODO delete
            //Thread.sleep(1000) // TODO delete

            // transit to the next state
            sys.transit(nextAct.get())
        }
    }

    override fun toString(): String {
        return "Proc($name)"
    }
}
