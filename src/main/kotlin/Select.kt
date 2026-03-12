import java.util.*
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
                    assert(select.winner.get() == chan.hashCode())
                    assert(done)
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
}

