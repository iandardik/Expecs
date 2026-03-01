import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class CompositeCondition(conditions : List<Condition>) {
    private val condWrappers = conditions.map { CondWrapper(this, it) }
    private val globalLock = ReentrantLock()
    private val globalCond = globalLock.newCondition()

    fun await() {
        try {
            globalLock.lock()
            condWrappers.forEach { Thread(it).start() }
            globalCond.await()
        }
        finally {
            globalLock.unlock()
        }
    }

    private class CondWrapper(
        private val compositeCond : CompositeCondition,
        private val cond : Condition
    ) : Runnable {
        override fun run() {
            cond.await()
            compositeCond.globalCond.signalAll()
        }
    }
}