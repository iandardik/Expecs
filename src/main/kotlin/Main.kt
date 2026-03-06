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

    override fun isEnabled(a: Action): Boolean {
        return true
    }

    override fun transit(a: Action) {
        ++numActs
        println("$name: $a")
    }

}

fun main() {
    val chan1 = Channel<Int>()
    val chan2 = Channel<Int>()
    for (i in 1..5) {
        //println("\n\n\n")
        val t1 = Thread {
            //chan1.send(1)
            Select(
                Select.SendCase(chan1, 1) { },
                Select.SendCase(chan2, 2) { },
            ).run()
            /*
            Select(
                Select.ReceiveCase(chan1) { data -> println("t1 chan1: $data") },
                Select.ReceiveCase(chan2) { data -> println("t1 chan2: $data") },
            ).run()
             */
        }
        val t2 = Thread {
            //println("t2 chan1: ${chan1.receive()}")
            Select(
                Select.ReceiveCase(chan1) { data -> println("t2 chan1: $data") },
                Select.ReceiveCase(chan2) { data -> println("t2 chan2: $data") },
            ).run()
            /*
            Select(
                Select.SendCase(chan1, 3) { },
                Select.SendCase(chan2, 4) { },
            ).run()
             */
        }

        val tpool = Executors.newFixedThreadPool(2)
        tpool.submit(t1)
        tpool.submit(t2)
        tpool.shutdown()
        t1.join()
        t2.join()
    }
}

fun main4() {
    val chan1 = Channel<Int>()
    for (i in 1..1) {
        println("i: $i")
        val t1 = Thread {
            Select(
                Select.SendCase(chan1, i) { },
            ).run()
        }
        val t2 = Thread {
            Select(
                Select.ReceiveCase(chan1) { data -> println("t2 chan1: $data") },
            ).run()
        }

        val tpool = Executors.newFixedThreadPool(2)
        tpool.submit(t1)
        tpool.submit(t2)
        tpool.shutdown()
        t1.join()
        t2.join()
    }
}

fun main3() {
    val chan1 = Channel<Int>()
    val chan2 = Channel<Int>()
    val chan3 = Channel<Int>()
    val t1 = Thread {
        Select(
            Select.SendCase(chan1, 1) { },
            Select.SendCase(chan2, 2) { },
            Select.ReceiveCase(chan3) { data -> println("t1 chan3: $data") },
        ).run()
    }
    val t2 = Thread {
        Select(
            Select.ReceiveCase(chan1) { data -> println("t2 chan1: $data") },
            Select.ReceiveCase(chan2) { data -> println("t2 chan2: $data") },
            Select.SendCase(chan3, 3) { },
        ).run()
    }

    val tpool = Executors.newFixedThreadPool(3)
    tpool.submit(t1)
    tpool.submit(t2)
    tpool.shutdown()
}

fun main2() {
    val chan1 = Channel<Int>()
    val chan2 = Channel<Int>()
    val t1 = Thread {
        chan1.send(2)
        chan1.send(4)
        chan1.send(6)
        println("t1: " + chan2.receive())
        println("t1: " + chan2.receive())
        println("t1: " + chan2.receive())
    }
    val t2 = Thread {
        println("t2: " + chan1.receive())
        println("t2: " + chan1.receive())
        println("t2: " + chan1.receive())
        chan2.send(1)
        chan2.send(3)
        chan2.send(5)
    }

    val tpool = Executors.newFixedThreadPool(3)
    tpool.submit(t1)
    tpool.submit(t2)
    tpool.shutdown()
}

fun main1() {
    val a = Action("a")
    val b = Action("b")
    val c = Action("c")
    val d = Action("d")

    //val p1 = PrintProc("P1", setOf(a,b,c))
    //val p2 = PrintProc("P2", setOf(a,b,d))
    val p1 = PrintProc("P1", setOf(a,b))
    val p2 = PrintProc("P2", setOf(a,c))

    val t1 = Thread(p1)
    val t2 = Thread(p2)
    val tpool = Executors.newFixedThreadPool(2)
    tpool.submit(t1)
    tpool.submit(t2)
    tpool.shutdown()
}