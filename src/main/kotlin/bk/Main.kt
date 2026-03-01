package bk

import java.util.*
import java.util.concurrent.Executors

class PrintProc(name : String, alphabet : Set<Action>) : Proc(name, alphabet) {
    private var numActs = 1

    override fun nextScheduledAction(): Optional<Action> {
        // only allow 4 actions
        if (numActs > 4) {
            return Optional.empty()
        }
        return Optional.of(alphabet.random())
    }

    override fun transit(a: Action) {
        ++numActs
        println("$name: $a")
    }

}

fun main() {
    val a = Action("a", 2)
    val b = Action("b", 2)
    val c = Action("c", 1)
    val d = Action("d", 1)

    val p1 = PrintProc("P1", setOf(a,b,c))
    val p2 = PrintProc("P2", setOf(a,b,d))

    val t1 = Thread(p1)
    val t2 = Thread(p2)
    val tpool = Executors.newFixedThreadPool(2)
    tpool.submit(t1)
    tpool.submit(t2)
    tpool.shutdown()
}
