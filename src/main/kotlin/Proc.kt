import java.util.*

abstract class Proc(
    val name : String,
    val alphabet: Set<Action>) : Runnable
{
    private val reqChan = Channel<Action>()
    private val ackChan = Channel<Boolean>()
    init {
        alphabet.forEach { a -> a.addProc(this) }
    }

    abstract fun nextScheduledAction() : Optional<Action>
    abstract fun isEnabled(a : Action) : Boolean
    abstract fun transit(a : Action)

    override fun run() {
        while (true) {
            val nextAct = nextScheduledAction()
            if (nextAct.isEmpty) {
                // deadlock reached! terminate the process
                println("$name: exiting")
                return
            }

            val act = nextAct.get()
            if (!act.mustSync()) {
                transit(act)
                continue
            }

            val syncProc = act.mustSyncWith(this)
            Select(
                // synchronous action
                Select.SendCase(syncProc.reqChan, act) {
                    //println("send $act")
                    transit(act)
                    /*
                    val ack = ackChan.receive()
                    if (ack) {
                        transit(act)
                        ackChan.send(true)
                    }
                     */
                },
                // a sync request is waiting (ignore <act>)
                Select.ReceiveCase(reqChan) {
                    reqAct ->
                        //println("rec $reqAct")
                        transit(act)
                        /*
                        if (isEnabled(reqAct)) {
                            transit(reqAct)
                            ackChan.send(true)
                            ackChan.receive()
                        }
                        else {
                            ackChan.send(false)
                        }
                         */
                }
            ).run()
        }
    }

    override fun toString(): String {
        return name
    }
}