import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.jvm.optionals.getOrDefault

class SyncChannel<T>(private val syncSize : Int) {
    private val lobbyLock = ReentrantLock()
    private val lobbyCond = lobbyLock.newCondition()
    private val comLock = ReentrantLock()
    private val comCond = comLock.newCondition()

    private var size = 0
    private var syncValue = Optional.empty<T>()
    private var commitVotes = 0
    private var aborted = false
    private var numExited = 0

    fun sync(compute : ()->T, commit : (T)->Boolean = {true}) : Optional<T> {
        // wait to enter the channel
        try {
            lobbyLock.lock()

            // waiting in the "lobby" to get in
            while (size == syncSize) {
                lobbyCond.await()
            }

            // the thread has gotten "in", now wait until enough threads have also gotten in
            ++size
            // TODO add the formula to some shared DS
            if (size == syncSize) {
                lobbyCond.signalAll()
            } else {
                lobbyCond.await()
            }
        }
        finally {
            lobbyLock.unlock()
        }

        // the channel has been entered
        try {
            comLock.lock()

            // the first thread to enter this critical section will compute SAT on all formulas
            if (syncValue.isEmpty) {
                syncValue = Optional.of(compute.invoke())
            }

            // TODO if the syncValue is UNSAT then we should retry

            // attempt to commit to the value
            val commit = commit.invoke(syncValue.get())
            if (commit) {
                ++commitVotes
            }
            aborted = aborted || !commit
            // wait until an abort or enough votes to commit the value
            while (!aborted && !(commit && commitVotes == syncSize)) {
                comCond.await()
            }
            comCond.signalAll()

            return if (aborted) {
                Optional.empty<T>()
            } else {
                // commit
                syncValue
            }
        }
        finally {
            ++numExited
            if (numExited == syncSize) {
                // the last one out cleans up
                commitVotes = 0
                aborted = false
                syncValue = Optional.empty()
                numExited = 0
                try {
                    lobbyLock.lock()
                    size = 0

                    // tell everyone in the lobby that we're done
                    lobbyCond.signalAll()
                }
                finally {
                    lobbyLock.unlock()
                }
            }
            comLock.unlock()
        }
    }
}

fun mainA() {
    val chan1 = SyncChannel<Int>(2)
    for (i in 0.. 100) {
        val randGen = { Random().nextInt() }
        val t1 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t1: $rv")
        }
        val t2 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t2: $rv")
        }
        val t3 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t3: $rv")
        }
        val t4 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t4: $rv")
        }

        val tpool = Executors.newFixedThreadPool(100)
        tpool.submit(t1)
        tpool.submit(t2)
        tpool.submit(t3)
        tpool.submit(t4)
        tpool.shutdown()
    }
}

fun main() {
    val chan1 = SyncChannel<Int>(2)
    for (i in 0.. 100) {
        val randGen = { Random().nextInt() }
        val ffun : (Int)->Boolean = { false }
        val t1 = Thread {
            val rv = chan1.sync(randGen,ffun).get()
            println("t1: $rv")
        }
        val t2 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t2: $rv")
        }
        val t3 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t3: $rv")
        }
        val t4 = Thread {
            val rv = chan1.sync(randGen).get()
            println("t4: $rv")
        }

        val tpool = Executors.newFixedThreadPool(100)
        tpool.submit(t1)
        tpool.submit(t2)
        tpool.submit(t3)
        tpool.submit(t4)
        tpool.shutdown()
    }
}
