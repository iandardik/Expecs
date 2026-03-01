import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

class MonitoredCondition(
    private val cond : Condition,
    private var callback : ()->Unit
) : Condition {
    override fun await() {
        cond.await()
        callback.invoke()
    }

    override fun await(time: Long, unit: TimeUnit?): Boolean {
        val rv = cond.await(time, unit)
        callback.invoke()
        return rv
    }

    override fun awaitUninterruptibly() {
        cond.awaitUninterruptibly()
        callback.invoke()
    }

    override fun awaitNanos(nanosTimeout: Long): Long {
        val rv = cond.awaitNanos(nanosTimeout)
        callback.invoke()
        return rv
    }

    override fun awaitUntil(deadline: Date): Boolean {
        val rv = cond.awaitUntil(deadline)
        callback.invoke()
        return rv
    }

    override fun signal() {
        cond.signal()
    }

    override fun signalAll() {
        cond.signalAll()
    }
}