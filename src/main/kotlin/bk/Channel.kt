package bk

import java.util.*
import java.util.concurrent.locks.ReentrantLock

class Channel<T> {
    val lock = ReentrantLock()
    val sendCondition = lock.newCondition()
    val recvCondition = lock.newCondition()

    var transmission = Optional.empty<T>()

    fun send(data : T) {
        try {
            lock.lock()
            recvCondition.signal() // notify a receiver that we are ready to send
            sendCondition.await() // wait until a receiver is ready

            // set the transmission value and exit
            transmission = Optional.of(data)
            recvCondition.signal()
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
            return recv
        }
        finally {
            lock.unlock()
        }
    }
}
