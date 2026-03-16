import tlc2.tool.Action
import tlc2.tool.TLCState
import tlc2.tool.impl.FastTool
import java.lang.Exception
import java.util.*

class Proc(
    private val name : String,
    private val tool : FastTool,
    private val alphabet : Set<SyncAction>
) : Runnable {
    private val randGen = Random()
    private var curState : TLCState

    init {
        // set up the initial state
        val initStateVec = tool.initStates
        val initStateIdx = randGen.nextInt(0, initStateVec.size())
        curState = initStateVec.elementAt(initStateIdx)
        println("proc $name initState:\n$curState")
    }

    override fun run() {
        try {
            println("proc $name curState:\n$curState")
        } catch (e : Exception) {
            println("proc $name: exception while getting cur state:")
            e.printStackTrace()
        }
        return
        while (true) {
            var nextAct = Optional.empty<Action>()
            val cases = alphabet.map {
                val enabledActs = concreteEnabledActions(it)
                println("proc $name enab: $enabledActs")
                Select.SyncCase(it.channel, enabledActs) { syncAct ->
                    /*if (syncActs.isNotEmpty()) {
                        nextAct = Optional.of(syncActs.random())
                    }*/
                    nextAct = Optional.of(syncAct)
                }
            }
            Select(*cases.toTypedArray()).run()

            // check for deadlocks
            if (nextAct.isEmpty) {
                println("$name: Deadlock")
                return
            }
            println("$name: ${nextAct.get()}")
            Thread.sleep(5000)

            // transit to the next state
            val succStates = tool.actions
                .flatMap { a ->
                    stateVecToList(tool.getNextStates(a,curState)).map { succ -> Pair(a,succ) }
                }
                .filter { (act,_) -> act == nextAct.get() }
                .map { (_,succ) -> succ }
                .toSet()
            assert(succStates.isNotEmpty())
            val nextState = succStates.random()
            curState = nextState
        }
    }

    private fun concreteEnabledActions(act : SyncAction) : Set<Action> {
        val interm = tool.actions
            .flatMap { a ->
                stateVecToList(tool.getNextStates(a,curState)).map { succ -> Triple(prettyAction(a),a,succ) }
            }
        println("proc $name curState:\n$curState")
        println("proc $name interm: $interm")
        return tool.actions
            .flatMap { a ->
                stateVecToList(tool.getNextStates(a,curState)).map { succ -> Pair(a,succ) }
            }
            .filter { (a,_) -> act.getName() == a.name.toString() }
            .filter { (_,succ) -> tool.isGoodState(succ) }
            .map { (a,_) -> a }
            .toSet()
    }

    override fun toString(): String {
        return "Proc($name,$alphabet)"
    }
}
