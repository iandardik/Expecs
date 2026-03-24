package exspecs.program

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import exspecs.concurrency.SyncChannel
import java.util.*

/**
 * A program represents one or more processes that interact together on a single computer.
 */
class Program : Runnable {
    private val procs : Set<Proc>

    /**
     * The constructor sets up a channel for each SymbolicAction so that each process that engages in the action can
     * communicate (synchronize on args) over the channel.
     */
    constructor(components : Set<TransitionSystem>) {
        // all action signatures that have the same name should have the same param
        // TODO add a sanity check for the above requirement
        val actionBag = components.flatMap { it.alphabet().map { act -> act.signature } }
        val actionCounts = actionBag.toSet().associateWith { setAct -> actionBag.count { bagAct -> bagAct == setAct } }
        val channelTable = actionCounts.keys.associateWith { act ->
            val syncSize = actionCounts[act]!!
            val ctx = Context() // one Context per channel
            SyncChannel<ConcreteAction,BoolExpr>(syncSize) { constraints ->
                val solver = ctx.mkSolver()
                // c.translate(ctx) is key because each constraint will come from a different thread, and hence are
                // created by different Contexts.
                constraints.forEach { c -> solver.add(c.translate(ctx)) }
                if (solver.check() == Status.SATISFIABLE) {
                    // TODO make this generic, rather than specific to Ints
                    val valueMap = act.args.associateWith {
                        val valExpr = solver.model.eval(ctx.mkIntConst(it), true)
                        Integer.parseInt(valExpr.toString())
                    }
                    Optional.of(ConcreteAction(act, valueMap))
                } else {
                    Optional.empty()
                }
            }
        }
        procs = components.map { Proc(it,channelTable) }.toSet()
    }

    override fun run() {
        val threads = procs.map { Thread(it) }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}