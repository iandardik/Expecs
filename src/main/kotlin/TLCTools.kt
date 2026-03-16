import tlc2.tool.Action
import tlc2.tool.StateVec
import tlc2.tool.TLCState
import tlc2.tool.impl.FastTool
import util.FileUtil
import util.SimpleFilenameToStream
import java.util.*

class TLAInterp {
}

/**
 * @param path the path to the TLA+ file (only the directory is needed, but a full path is ok)
 * @param module the TLA+ module (the TLA+ file without the path or .tla extension)
 * @param config the name of the config file (the config file without the path or .cfg extension)
 */
fun parseTLA(path : String, module : String, config : String) : FastTool {
    val dir = FileUtil.parseDirname(path);
    val resolver =
        if (dir.isNotEmpty()) {
            SimpleFilenameToStream(dir)
        } else {
            SimpleFilenameToStream()
        }

    return FastTool(module, config, resolver)
}

fun prettyAction(act : Action) : String {
    val params = act.opDef.params.map {
        act.con.lookup(it)
    }.joinToString(",") {
        it.toString()
    }
    return act.name.toString() + "(" + params + ")"
}

fun stateVecToList(vec : StateVec) : List<TLCState> {
    val list = mutableListOf<TLCState>()
    for (i in 0 until vec.size()) {
        list.add(vec.elementAt(i))
    }
    return list
}

fun main1() {
    val tlaFile = "/Users/idardik/Documents/CMU/compositional_ii/carini/case_studies/two_phase/verification/RM.tla"
    val tla = "/Users/idardik/Documents/CMU/compositional_ii/carini/case_studies/two_phase/verification/RM"
    val config = "/Users/idardik/Documents/CMU/compositional_ii/carini/case_studies/two_phase/verification/TwoPhase"

    val dir = FileUtil.parseDirname(tlaFile);
    val resolver =
        if (dir.isNotEmpty()) {
            SimpleFilenameToStream(dir)
        } else {
            SimpleFilenameToStream()
        }

    val tool = FastTool(tla, config, resolver)
    val initStateVec = tool.initStates
    println()
    println("init states:")
    for (i in 0 until initStateVec.size()) {
        val s = initStateVec.elementAt(i)
        println(s)
    }

    println("actions:")
    for (act in tool.actions) {
        val params = act.opDef.params.map {
            //it.signature
            act.con.lookup(it)
        }.joinToString(",") {
            it.toString()
        }
        val name = act.name.toString() + "(" + params + ")"
        println(name)
    }
}

fun main() {
    val randGen = Random()

    val path = "/Users/idardik/Documents/CMU/compositional_ii/carini/case_studies/two_phase/verification/"
    val tla = "RM"
    val config = "TwoPhase"

    val tool = parseTLA(path, tla, config)
    val initStateVec = tool.initStates
    val initStateIdx = randGen.nextInt(0, initStateVec.size())
    val initState = initStateVec.elementAt(initStateIdx)
    println("init state: $initState")

    var curState = initState
    while (true) {
        val enabledActionSuccPairs = tool.actions
            .flatMap { a ->
                stateVecToList(tool.getNextStates(a,curState)).map { succ -> Pair(a,succ) }
            }
            .filter { (_,succ) -> tool.isGoodState(succ) }
            .toSet()

        val (nextAct, nextState) = enabledActionSuccPairs.random()
        curState = nextState

        println(prettyAction(nextAct))
        println(nextState)

        Thread.sleep(2000)
    }
}
