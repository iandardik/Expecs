import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class Select(private vararg val cases : Case) : Runnable {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val casesLock = StratifiedLock()
    private val threads = cases.map {
        val t = Thread(it)
        it.setThread(t)
        t
    }
    var selectionMade = false

    override fun run() {
        // TODO check to make sure no two cases use the same channel
        // TODO make sure that each cases isn't already associated with a select

        cases.forEach { it.setSelect(this) }

        //val threads = cases.map { Thread(it) }
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
        assert(selectionMade)
        threads.forEach {
            //it.interrupt()
        }
    }


    interface Case : Runnable {
        fun setSelect(s : Select)
        fun setThread(t : Thread)
    }
    class SendCase<T>(
        private val chan : Channel<T>,
        private val data : T,
        private val callback : ()->Unit
    ) : Case {
        private var selectRef : Optional<Select> = Optional.empty()
        private var threadRef : Optional<Thread> = Optional.empty()


        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }

        override fun setThread(t : Thread) {
            threadRef = Optional.of(t)
        }

        override fun run() {
            val select = selectRef.get()
            var iAmSelected = false
            val sent = chan.send(data, setOf(select.casesLock)) {
                if (!select.selectionMade || iAmSelected) {
                    select.selectionMade = true
                    iAmSelected = true
                    select.threads
                        .filter { it != threadRef.get() }
                        .forEach { it.interrupt() }
                    return@send true
                } else {
                    return@send false
                }
            }

            // if the data was sent then this case was chosen, so invoke the callback
            if (sent) {
                callback.invoke()

                // let the main thread know we're done
                try {
                    select.lock.lock()
                    select.condition.signalAll()
                }
                finally {
                    select.lock.unlock()
                }
            }
        }
    }
    class ReceiveCase<T>(
        private val chan : Channel<T>,
        private val callback : (T)->Unit
    ) : Case {
        private var selectRef : Optional<Select> = Optional.empty()
        private var threadRef : Optional<Thread> = Optional.empty()

        override fun setSelect(s : Select) {
            selectRef = Optional.of(s)
        }

        override fun setThread(t : Thread) {
            threadRef = Optional.of(t)
        }

        override fun run() {
            val select = selectRef.get()
            var iAmSelected = false
            val recv = chan.receive(setOf(select.casesLock)) {
                if (!select.selectionMade || iAmSelected) {
                    select.selectionMade = true
                    iAmSelected = true
                    select.threads
                        .filter { it != threadRef.get() }
                        .forEach { it.interrupt() }
                    return@receive true
                } else {
                    return@receive false
                }
            }

            // if the data was sent received then this case was chosen, so invoke the callback
            if (recv.isPresent) {
                callback.invoke(recv.get())

                // let the main thread know we're done
                try {
                    select.lock.lock()
                    select.condition.signalAll()
                }
                finally {
                    select.lock.unlock()
                }
            }
        }

    }
}