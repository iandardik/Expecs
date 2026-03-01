import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class Select(vararg cases : Case) : Runnable {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    var selectionMade = false

    // important for 2PL to always lock in the same order across all Select objects
    private val cases = cases.sortedBy { it.getId() }

    override fun run() {
        // check to make sure no two cases use the same channel
        val multipleCases = cases
            .map { it.getId() }
            .fold(Pair(emptySet<Int>(),false)) {
                acc,id ->
                    Pair(acc.first union setOf(id), acc.second || id in acc.first)
            }
            .second
        assert(!multipleCases)

        // TODO make sure that each cases isn't already associated with a select

        cases.forEach { it.setSelect(this) }

        val threads = cases.map { Thread(it) }
        try {
            // spawn a thread for each case and listen on the channel. each thread attempts to "win" the select statement
            // by communicating with its channel first. a winner may be chosen by a different select statement, however,
            // if it communicates first with one of the channels in "this" object.
            lock.lock()
            threads.forEach { it.start() }
            condition.await()
        }
        finally {
            lock.unlock()
        }
        threads.forEach {
            it.interrupt()
        }
    }


    interface Case : Runnable {
        fun setSelect(s : Select)
        fun getId() : Int
        fun getLock() : Lock
        fun getWinners() : Set<Int>
        fun addWinner(id : Int)
        fun removeWinner(id : Int)
    }
    class SendCase<T>(
        private val chan : Channel<T>,
        private val data : T,
        private val callback : ()->Unit
    ) : Case {
        private var selectRef : Optional<Select> = Optional.empty()

        override fun setSelect(s: Select) {
            selectRef = Optional.of(s)
        }

        override fun getId(): Int {
            return chan.id
        }

        override fun getLock(): Lock {
            return chan.lock
        }

        override fun getWinners(): Set<Int> {
            return chan.selectWinners
        }

        override fun addWinner(id: Int) {
            chan.selectWinners = chan.selectWinners union setOf(id)
        }

        override fun removeWinner(id: Int) {
            chan.selectWinners = chan.selectWinners.minus(id)
        }

        override fun run() {
            val select = selectRef.get()
            try {
                chan.lock.lock()
                while (true) {
                    chan.recvCondition.signal() // notify a receiver that we are ready to send
                    chan.sendCondition.await() // wait until a receiver is ready

                    // only let the first case to fire by performing a Two Phase Lock (2PL) to avoid race conditions.
                    // perform 2PL for all cases that are "downstream" (larger than or equal) to this one. however, we
                    // only lock the "strictly downstream" cases at this point because the current case is already locked.
                    val downstreamCases = select.cases
                        .filter { this.getId() <= it.getId() }
                    val strictlyDownstreamLocks = select.cases
                        .filter { this.getId() < it.getId() }
                        .map { it.getLock() }
                    try {
                        strictlyDownstreamLocks.forEach { it.lock() }
                        select.lock.lock()

                        // check each case for winners that overlap with this select
                        val winners = downstreamCases
                            .map { it.getWinners() }
                            .foldRight(emptySet<Int>()) { x,acc -> x union acc }
                        val selectWinners = winners intersect select.cases.map { it.getId() }.toSet()
                        val thisIsTheWinner = getId() in selectWinners
                        if (!select.selectionMade && selectWinners.isEmpty()) {
                            // this thread is the winner
                            select.selectionMade = true
                            downstreamCases.forEach { it.addWinner(this.getId()) }
                        }
                        else if (thisIsTheWinner) {
                            // this case is a winner, chosen by a different select statement
                            assert(!select.selectionMade)
                            select.selectionMade = true
                        }
                        else if (select.selectionMade) {
                            // abort the send
                            return
                        }
                        else {
                            // otherwise, retry the send
                            continue
                        }
                    }
                    finally {
                        select.lock.unlock()
                        strictlyDownstreamLocks.forEach { it.unlock() }
                    }

                    // set the transmission value
                    chan.transmission = Optional.of(data)
                    chan.recvCondition.signal()

                    // wait for the receiver to send an ack--retry the send if the transmission value is still present
                    chan.ackCondition.await()
                    if (chan.transmission.isEmpty) {
                        removeWinner(getId())
                        downstreamCases.forEach {
                            try {
                                it.getLock().lock()
                                it.removeWinner(getId())
                            } finally {
                                it.getLock().unlock()
                            }
                        }

                        // invoke the callback now that the transmission has been sent
                        callback.invoke()
                        try {
                            select.lock.lock()
                            select.condition.signalAll()
                        }
                        finally {
                            select.lock.unlock()
                        }
                        return
                    } else {
                        chan.transmission = Optional.empty()
                    }
                }
            }
            catch (e : InterruptedException) {
                return
            }
            finally {
                chan.lock.unlock()
            }
        }
    }
    class ReceiveCase<T>(
        private val chan : Channel<T>,
        private val callback : (T)->Unit
    ) : Case {
        private var selectRef : Optional<Select> = Optional.empty()

        override fun setSelect(s: Select) {
            selectRef = Optional.of(s)
        }

        override fun getId(): Int {
            return chan.id
        }

        override fun getLock(): Lock {
            return chan.lock
        }

        override fun getWinners(): Set<Int> {
            return chan.selectWinners
        }

        override fun addWinner(id: Int) {
            chan.selectWinners = chan.selectWinners union setOf(id)
        }

        override fun removeWinner(id: Int) {
            chan.selectWinners = chan.selectWinners.minus(id)
        }

        override fun run() {
            val select = selectRef.get()
            val downstreamCases = select.cases
                .filter { this.getId() <= it.getId() }
            val strictlyDownstreamLocks = select.cases
                .filter { this.getId() < it.getId() }
                .map { it.getLock() }

            try {
                chan.lock.lock()
                while (chan.transmission.isEmpty) {
                    chan.sendCondition.signal() // notify a sender that we are ready to receive
                    chan.recvCondition.await() // wait until a sender is ready

                    // only let the first case to fire by performing a Two Phase Lock (2PL) to avoid race conditions.
                    // perform 2PL for all cases that are "downstream" (larger than or equal) to this one. however, we
                    // only lock the "strictly downstream" cases at this point because the current case is already locked.
                    try {
                        strictlyDownstreamLocks.forEach { it.lock() }
                        select.lock.lock()

                        // check each case for winners that overlap with this select
                        val winners = downstreamCases
                            .map { it.getWinners() }
                            .foldRight(emptySet<Int>()) { x,acc -> x union acc }
                        val selectWinners = winners intersect select.cases.map { it.getId() }.toSet()
                        val thisIsTheWinner = getId() in selectWinners
                        if (!select.selectionMade && selectWinners.isEmpty() && chan.transmission.isPresent) {
                            // this thread is the winner
                            select.selectionMade = true
                            downstreamCases.forEach { it.addWinner(this.getId()) }
                        }
                        else if (thisIsTheWinner) {
                            // this case is a winner, chosen by a different select statement
                            assert(!select.selectionMade)
                            select.selectionMade = true
                        }
                        else if (select.selectionMade) {
                            // abort the send
                            return
                        }
                        else {
                            // otherwise, retry the send
                            continue
                        }
                    }
                    finally {
                        select.lock.unlock()
                        strictlyDownstreamLocks.forEach { it.unlock() }
                    }
                }

                // receive the transmission value then reset it to empty
                val recv = chan.transmission.get()
                chan.transmission = Optional.empty()

                // send an ack to the sender before exiting
                chan.ackCondition.signalAll()

                // clean up
                removeWinner(getId())
                downstreamCases.forEach {
                    try {
                        it.getLock().lock()
                        it.removeWinner(getId())
                    } finally {
                        it.getLock().unlock()
                    }
                }

                callback.invoke(recv)
                try {
                    select.lock.lock()
                    select.condition.signalAll()
                }
                finally {
                    select.lock.unlock()
                }
            }
            catch (e : InterruptedException) {
                return
            }
            finally {
                chan.lock.unlock()
            }
        }
    }
}