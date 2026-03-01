package bk

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class Action(
    private val name : String,
    private val numSyncProcs : Int)
{
    private val syncProcs = mutableSetOf<Proc>()
    private val lock : ReentrantLock = ReentrantLock()
    private val condition : Condition = lock.newCondition()

    fun engage(p : Proc) : Boolean {
        try {
            lock.lock()
            syncProcs.add(p)
            return if (syncProcs.size == numSyncProcs) {
                // all procs are synchronized
                condition.signalAll()
                true
            } else {
                // wait for the remaining procs to synchronize
                val allSynced = condition.await(1, TimeUnit.SECONDS)
                allSynced
            }
        }
        finally {
            syncProcs.remove(p)
            lock.unlock()
        }
    }

    override fun toString(): String {
        return name
    }
}
