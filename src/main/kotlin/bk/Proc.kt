package bk

import java.util.*

abstract class Proc(
    val name : String,
    val alphabet: Set<Action>) : Runnable
{

    abstract fun nextScheduledAction() : Optional<Action>
    abstract fun transit(a : Action)

    override fun run() {
        while (true) {
            val nextAct = nextScheduledAction()
            if (nextAct.isEmpty) {
                // deadlock reached! terminate the process
                return
            }

            val act = nextAct.get()
            val ok = act.engage(this)
            if (ok) {
                transit(act)
            }
        }
    }

    override fun toString(): String {
        return name
    }
}
