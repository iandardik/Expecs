class Sys {
}

fun main() {
    val folder = "/Users/idardik/Documents/CMU/compositional_ii/carini/examples/toy_two_phase/"
    val rmSpec = Triple(folder, "RM", "ToyTwoPhase")
    val tmSpec = Triple(folder, "TM", "no_invs")

    //val rmTool = parseTLA(rmSpec.first, rmSpec.second, rmSpec.third)
    //val tmTool = parseTLA(tmSpec.first, tmSpec.second, tmSpec.third)

    val strActionMap = listOf(rmSpec,tmSpec)
        .associate { (path, module, config) ->
            val tool = parseTLA(path, module, config)
            val alph = tool.actions
                .map { it.name.toString() }
                .toSet()
            Pair(module, alph)
        }

    val strActions = strActionMap.values.flatten()
    val actionCountMap = strActions.toSet()
        .associateWith { act ->
            strActions.count { e -> e == act }
        }
    val globalAlph = strActions.toSet()
        .map { act ->
            actionCountMap[act]?.let { SyncAction(act, it) }
        }
        .toSet()
    val procs = listOf(rmSpec,tmSpec)
        .map {  (path, module, config) ->
            val tool = parseTLA(path, module, config)
            val strAlph = tool.actions
                .map { it.name.toString() }
                .toSet()
            val alph = globalAlph
                .filterNotNull()
                .filter { strAlph.contains(it.getName()) }
                .toSet()
            Proc(module, tool, alph)
        }

    println()
    val threads = procs.map { Thread(it) }
    threads.forEach { it.run() }
    threads.forEach { it.join() }
}
