package exspecs.concurrency

import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class Select(private vararg val cases : Case) : Runnable {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val publicLock = StratifiedLock()
    private var winner = Optional.empty<Int>()

    init {
        // check to make sure no two cases use the same channel
        val numCases = cases.size
        val numChannels = cases.map { it.getChannelHash() }.toSet().size
        if (numCases != numChannels) {
            throw RuntimeException("Each Case in a exspecs.concurrency.Select must use a unique channel")
        }

        // make sure that each cases isn't already associated with a select
        cases.forEach {
            if (it.hasSelect()) {
                throw RuntimeException("A Case must only be associated with a single exspecs.concurrency.Select")
            }
        }

        cases.forEach { it.setSelect(this) }
    }

    fun getPublicLock() = publicLock
    fun canCommit(chanHash : Int) : Boolean {
        return winner.isEmpty || winner.get() == chanHash
    }
    fun doCommit(chanHash : Int) {
        exspecs.tools.assert(canCommit(chanHash))
        winner = Optional.of(chanHash)
    }

    override fun run() {
        // make sure that run() is only ever run once
        if (winner.isPresent) {
            throw RuntimeException("exspecs.concurrency.Select run multiple times")
        }
        if (cases.isEmpty()) {
            return
        }

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
        exspecs.tools.assert(winner.isPresent)
        threads.forEach {
            it.interrupt()
        }
    }


    interface Case : Runnable {
        fun setSelect(s : Select)
        fun hasSelect() : Boolean
        fun getChannelHash() : Int
    }
    class SyncCase<V : Any, C : Any>(
        private val chan : SyncChannel<V, C>,
        private val constraint : Optional<C>,
        private val callback : (V)->Unit = {}
    ) : Case {
        private var selectRef = Optional.empty<Select>()
        constructor(chan : SyncChannel<V, C>, callback : (V)->Unit = {})
            : this(chan, Optional.empty(), callback) {}
        constructor(chan : SyncChannel<V, C>, constraint : C, callback : (V)->Unit = {})
                : this(chan, Optional.of(constraint), callback) {}
        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }
        override fun hasSelect() = selectRef.isPresent
        override fun getChannelHash() = chan.hashCode()
        override fun run() {
            val select = selectRef.get()
            var done = false
            while (!done) {
                val ret = chan.sync(constraint, selectRef)
                done = ret.isPresent || select.winner.isPresent
                if (ret.isPresent) {
                    exspecs.tools.assert(select.winner.get() == chan.hashCode())
                    exspecs.tools.assert(done)
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

