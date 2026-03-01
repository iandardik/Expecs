import java.util.*
import java.util.concurrent.locks.ReentrantLock

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

class Channel<T> {
    val lock = ReentrantLock()
    val sendCondition = lock.newCondition()
    val recvCondition = lock.newCondition()
    val ackCondition = lock.newCondition()

    val id = nextId()
    var selectWinners = emptySet<Int>()
    var transmission = Optional.empty<T>()

    fun send(data : T) {
        try {
            lock.lock()
            while (true) {
                recvCondition.signal() // notify a receiver that we are ready to send
                sendCondition.await() // wait until a receiver is ready

                // set the transmission value
                transmission = Optional.of(data)
                recvCondition.signal()

                // wait for the receiver to send an ack--retry the send if the transmission value is still present
                ackCondition.await()
                if (transmission.isEmpty) {
                    return
                } else {
                    transmission = Optional.empty()
                }
            }
        }
        finally {
            lock.unlock()
        }
    }

    fun receive() : T {
        try {
            lock.lock()
            while (transmission.isEmpty) {
                sendCondition.signal() // notify a sender that we are ready to receive
                recvCondition.await() // wait until a sender is ready
            }

            // receive the transmission value then reset it to empty
            val recv = transmission.get()
            transmission = Optional.empty()

            // send an ack to the sender before exiting
            ackCondition.signalAll()
            return recv
        }
        finally {
            lock.unlock()
        }
    }
}
