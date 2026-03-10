import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

class Select(private vararg val cases : Case) : Runnable {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val publicLock = StratifiedLock()
    private var winner = Optional.empty<Int>()

    fun getPublicLock() = publicLock
    fun canCommit(chanHash : Int) : Boolean {
        return winner.isEmpty || winner.get() == chanHash
    }
    fun doCommit(chanHash : Int) {
        assert(canCommit(chanHash))
        winner = Optional.of(chanHash)
    }

    override fun run() {
        // TODO check to make sure no two cases use the same channel
        // TODO make sure that each cases isn't already associated with a select
        // TODO make sure that run() is only ever run once

        cases.forEach { it.setSelect(this) }
        val threads = cases.map { Thread(it) }
        try {
            // spawn a thread for each case and listen on the channel. each thread attempts to "win" the select statement
            // by communicating with its channel first.
            lock.lock()
            threads.forEach { it.start() }
            condition.await()
        }
        finally {
            lock.unlock()
        }
        assert(winner.isPresent)
        threads.forEach {
            it.interrupt()
        }
    }


    interface Case : Runnable {
        fun setSelect(s : Select)
    }
    class SyncCase<T : Any>(
        private val chan : SyncChannel<T>,
        private val callback : (T)->Unit
    ) : Case {
        private var selectRef = Optional.empty<Select>()
        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }
        override fun run() {
            val select = selectRef.get()
            var done = false
            while (!done) {
                val ret = chan.sync(selectRef)
                done = ret.isPresent || select.winner.isPresent
                if (ret.isPresent) {
                    callback.invoke(ret.get())
                    try {
                        select.lock.lock()
                        select.condition.signalAll()
                    } finally {
                        select.lock.unlock()
                    }
                }
            }
        }
    }

    class SendCase<T>(
        private val chan : Channel<T>,
        private val data : T,
        private val callback : ()->Unit
    ) : Case {
        private var selectRef : Optional<Select> = Optional.empty()
        private var threadRef : Optional<Thread> = Optional.empty()


        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }

        override fun run() {
        }
    }
    class ReceiveCase<T>(
        private val chan : Channel<T>,
        private val callback : (T)->Unit
    ) : Case {
        private var selectRef : Optional<Select> = Optional.empty()
        private var threadRef : Optional<Thread> = Optional.empty()

        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }

        override fun run() {
        }

    }
}

fun main() {
    val randGen = { Random().nextInt() }
    val chan1 = SyncChannel<Int>(2, randGen)
    val chan2 = SyncChannel<Int>(2, randGen)
    for (i in 1..100) {
        val t1 = Thread {
            Select(
                Select.SyncCase(chan1) { syncVal -> println("t1 chan1: $syncVal") },
                Select.SyncCase(chan2) { syncVal -> println("t1 chan2: $syncVal") },
            ).run()
        }
        val t2 = Thread {
            Select(
                Select.SyncCase(chan1) { syncVal -> println("t2 chan1: $syncVal") },
                Select.SyncCase(chan2) { syncVal -> println("t2 chan2: $syncVal") },
            ).run()
        }

        val tpool = Executors.newFixedThreadPool(100)
        tpool.submit(t1)
        tpool.submit(t2)
        tpool.shutdown()
        t1.join()
        t2.join()
    }
}
