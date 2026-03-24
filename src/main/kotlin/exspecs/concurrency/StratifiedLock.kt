package exspecs.concurrency

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class StratifiedLock : Lock, Comparable<StratifiedLock> {
    private val id = nextId()
    private val lock = ReentrantLock()

    fun getId() : Int = id

    override fun lock() {
        lock.lock()
    }

    override fun lockInterruptibly() {
        lock.lockInterruptibly()
    }

    override fun tryLock(): Boolean {
        return lock.tryLock()
    }

    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        return lock.tryLock(time, unit)
    }

    override fun unlock() {
        lock.unlock()
    }

    override fun newCondition(): Condition {
        return lock.newCondition()
    }

    override fun compareTo(other: StratifiedLock): Int {
        return getId().compareTo(other.getId())
    }
}

var globalId = 0
val globalLock = ReentrantLock()
fun nextId() : Int {
    try {
        globalLock.lock()
        return ++globalId
    }
    finally {
        globalLock.unlock()
    }
}
