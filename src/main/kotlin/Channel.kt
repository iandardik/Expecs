import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class Channel<T> {
    private val lock = ReentrantLock()
    private val sendCondition = lock.newCondition()
    private val recvCondition = lock.newCondition()
    private val sendAckCondition = lock.newCondition()
    private val recvAckCondition = lock.newCondition()
    private val recvLock = ReentrantLock()
    private val sendLock = ReentrantLock()

    private var twoPhaseLocks = emptySet<StratifiedLock>()
    private var sendAck = false
    private var recvAck = false
    private var transmission = Optional.empty<T>()

    private fun confirmAck(cfmCondition : ()->Boolean) : Boolean {
        var locksAcquired = emptySet<StratifiedLock>()
        try {
            // always sort to implement 2PL
            twoPhaseLocks.sorted().forEach {
                locksAcquired = locksAcquired.plus(it)
                it.lock()
            }
            return cfmCondition.invoke()
        }
        finally {
            locksAcquired.forEach { it.unlock() }
        }
    }

    // repeat the await() on the condition until it happens. return whether or not an interruption happened.
    private fun interruptionSafeAwait(cond : Condition) : Boolean {
        var done = false
        var interruption = false
        while (!done) {
            try {
                cond.await()
                done = true
            }
            catch (e : InterruptedException) {
                interruption = true
            }
            // TODO the bug here is that the other thread can signal <cond> before we await() on it again
        }
        return interruption
    }

    fun send(data : T, locks : Set<StratifiedLock> = emptySet(), cfmCondition : () -> Boolean = {true}) : Boolean {
        var interrupted = false
        try {
            sendLock.lock()
            lock.lock()
            while (true) {
                if (interrupted) {
                    return false
                }

                recvCondition.signal() // notify a receiver that we are ready to send
                // wait until a receiver is ready
                interrupted = interrupted || interruptionSafeAwait(sendCondition)

                // set any locks
                twoPhaseLocks = twoPhaseLocks union locks

                // set the transmission value
                transmission = Optional.of(data)
                recvCondition.signal()

                // wait for the receiver to send an ack--retry the send if the transmission value is still present
                interrupted = interrupted || interruptionSafeAwait(sendAckCondition)

                sendAck = !interrupted && confirmAck(cfmCondition) // perform all necessary locks before sending an ack back
                recvAckCondition.signalAll()
                twoPhaseLocks = twoPhaseLocks.minus(locks)
                transmission = Optional.empty()

                if (sendAck && recvAck) {
                    // success
                    //println("send succes")
                    return true
                }
                else if (!sendAck) {
                    // abort
                    //println("send aborting")
                    return false
                }
                else {
                    //println("send retry")
                    // retry
                }
            }
        }
        catch (e : InterruptedException) {
            //println("send interrupted")
            return false
        }
        finally {
            lock.unlock()
            sendLock.unlock()
        }
    }

    fun receive(locks : Set<StratifiedLock> = emptySet(), cfmCondition : () -> Boolean = {true}) : Optional<T> {
        var interrupted = false
        try {
            recvLock.lock()
            lock.lock()
            while (true) {
                assert(transmission.isEmpty)
                while (transmission.isEmpty) {
                    if (interrupted) {
                        return Optional.empty()
                    }

                    sendCondition.signal() // notify a sender that we are ready to receive
                    // wait until a sender is ready
                    interrupted = interrupted || interruptionSafeAwait(recvCondition)
                }

                // set any locks
                twoPhaseLocks = twoPhaseLocks union locks

                // receive the transmission value then reset it to empty
                val recv = transmission.get()
                transmission = Optional.empty()

                // perform all necessary locks before sending an ack back
                // TODO cfmCondition should accept recv
                recvAck = !interrupted && confirmAck(cfmCondition)
                sendAckCondition.signalAll()
                interrupted = interrupted || interruptionSafeAwait(recvAckCondition)
                twoPhaseLocks = twoPhaseLocks.minus(locks)

                if (recvAck && sendAck) {
                    // success
                    //println("recv success")
                    return Optional.of(recv)
                }
                else if (!recvAck) {
                    // abort
                    //println("recv aborting")
                    return Optional.empty()
                }
                else {
                    // retry
                    //println("recv retry")
                }
            }
        }
        catch (e : InterruptedException) {
            //println("recv interrupted")
            return Optional.empty()
        }
        finally {
            lock.unlock()
            recvLock.unlock()
        }
    }
}

fun main() {
    val chan1 = Channel<Int>()
    val t1 = Thread {
        chan1.send(2)
    }
    val t2 = Thread {
        println("t2: " + chan1.receive())
    }

    val tpool = Executors.newFixedThreadPool(3)
    tpool.submit(t1)
    tpool.submit(t2)
    tpool.shutdown()
}
