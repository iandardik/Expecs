package exspecs.concurrency

import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * V: The type of the value sent over the channel
 * C: The type of each constraint
 */
class SyncChannel<V : Any, C : Any>(
    private val syncSize : Int,
    private val compute : (Set<C>)->Optional<V>
) {
    // the shared variables used in the lobby
    private val lobbyLock = ReentrantLock()
    private val lobbyCond = lobbyLock.newCondition()
    private var size = 0
    private var constraints = emptySet<C>()
    private var selects : Set<Select> = emptySet()

    // the shared variables used for communication while attempting to sync
    private val comLock = ReentrantLock()
    private val comCond = comLock.newCondition()
    private var syncValue = SyncChannelResult.none<V>()
    private var commitVotes = 0
    private var aborted = false
    private var numExited = 0

    // the shared variable used for closing this channel
    private val closedLock = ReentrantLock()
    private var closed = false

    fun sync(select : Optional<Select> = Optional.empty(), retryOnUNSAT : Boolean = true) : SyncChannelResult<V> {
        return sync(Optional.empty(), select, retryOnUNSAT)
    }

    fun sync(constraint : C, select : Optional<Select> = Optional.empty(), retryOnUNSAT : Boolean = true) : SyncChannelResult<V> {
        return sync(Optional.of(constraint), select, retryOnUNSAT)
    }

    /**
     * This method will not check each constraint to see if it is satisfiable--that is up to the caller.
     */
    fun sync(constraint : Optional<C>, select : Optional<Select> = Optional.empty(), retryOnUNSAT : Boolean = true) : SyncChannelResult<V> {
        // TODO catch all exceptions in this method (instead of enterThroughLobby() and syncAttempt()) because an
        // TODO interruption can happen here too. Either way, we need to add more book keeping to our data structures
        // TODO so the exiting/interrupted thread can always exit safely.
        var attemptingSync = true
        var syncResult = SyncChannelResult.none<V>()
        while (attemptingSync) {
            // not the fairest policy to have each thread reenter the lobby on each retry
            val enter = enterThroughLobby(constraint, select, retryOnUNSAT)
            if (!enter) {
                return SyncChannelResult.abort()
            }

            // once we've made it here, attempt to sync
            val result = syncAttempt(select.isPresent)

            // retry syncing under the following two conditions:
            // 1. the result is a retry
            // 2. the result is UNSAT and we're in retryOnUNSAT mode
            val retry = result.isRetry || (result.isUNSAT && retryOnUNSAT)
            if (!retry) {
                attemptingSync = false
                syncResult = result
            }
        }
        return syncResult
    }

    /**
     * Returns whether the given constraint is mutually satisfiable with the current constraints in the lobby. Note that
     * we do not perform a check (and simply return true) if the current set of constraints is empty; this is safe based
     * on the assumption that each individual constraint is satisfiable, which we rely on for efficiency (to reduce the
     * number of calls to the SMT solver).
     */
    private fun satisfiableWithCurrentLobby(constraint : Optional<C>) : Boolean {
        if (constraint.isEmpty || constraints.isEmpty()) {
            return true
        }
        return compute.invoke(constraints.plus(constraint.get())).isPresent
    }

    private fun enterThroughLobby(constraint : Optional<C>, select : Optional<Select>, retryOnUNSAT : Boolean) : Boolean {
        // TODO figure out the reason this causes programs to hang, then put this back in to pass the "testClose1" test case
        /*if (checkIsClosed()) {
            return false
        }*/

        // wait to enter the channel
        lobbyLock.lock()
        try {
            // waiting in the "lobby" to get in
            while (size == syncSize || (retryOnUNSAT && !satisfiableWithCurrentLobby(constraint))) {
                lobbyCond.await()
                if (checkIsClosed()) {
                    return false
                }
            }

            // the thread has gotten "in", now wait until enough threads have also gotten in
            ++size
            if (constraint.isPresent) {
                // add the thread's value to the set of constraints
                constraints = constraints.plus(constraint.get())
            }
            if (select.isPresent) {
                selects = selects.plus(select.get())
            }
            if (size == syncSize) {
                lobbyCond.signalAll()
            } else {
                lobbyCond.await()
                if (checkIsClosed()) {
                    return false
                }
            }
            return true
        }
        catch (e : InterruptedException) {
            // there are two await() calls in this try, so two possible places where an interrupt can happen.
            // the first requires no clean up, but the second one does.
            // TODO to clean up after the second await(), we need to add more book keeping to size, constraints, and
            // TODO selects to keep track of each thread in the lobby, and we can remove them here so they correctly
            // TODO exit the lobby.
            return false
        }
        finally {
            lobbyLock.unlock()
        }
    }

    private fun exitThroughLobby() {
        lobbyLock.lock()
        try {
            size = 0
            constraints = emptySet()
            selects = emptySet()

            // tell everyone in the lobby that we're done
            lobbyCond.signalAll()
        }
        finally {
            lobbyLock.unlock()
        }
    }

    private fun selectsCommit() : Boolean {
        // request a commit from all parties--only commit if all are able to
        // 2PL on all selects
        val allLocks = selects.map { it.getPublicLock() }.sorted()
        try {
            allLocks.forEach { it.lock() }
            val allCanCommit = selects.all { it.canCommit(hashCode()) }
            if (allCanCommit) {
                selects.forEach { it.doCommit(hashCode()) }
            }
            return allCanCommit
        }
        finally {
            allLocks.forEach {
                if (it.isLocked()) {
                    it.unlock()
                }
            }
        }
    }

    private fun syncAttempt(hasSelect : Boolean) : SyncChannelResult<V> {
        // the channel has been entered
        comLock.lock()
        try {
            // the first thread to enter this critical section will compute SAT on all formulas
            if (syncValue.isEmpty) {
                val computeResult = compute.invoke(constraints)
                syncValue = if (computeResult.isPresent) {
                    SyncChannelResult.sat(computeResult.get())
                } else {
                    SyncChannelResult.unsat()
                }
            }

            // attempt to commit to the value
            val commit = selectsCommit()
            if (commit) {
                ++commitVotes
            }
            aborted = aborted || !commit
            // wait until an abort or enough votes to commit the value
            while (!aborted && commitVotes < syncSize) {
                // at this point, this thread is attempting to commit but doesn't have the votes yet to commit.
                // wait for the other threads to decide if they want to commit or abort.
                comCond.await()
                if (checkIsClosed()) {
                    return SyncChannelResult.abort()
                }
            }
            comCond.signalAll()

            return if (commit && !aborted) {
                syncValue
            } else if (commit && hasSelect) {
                // never retry if there's a select--the select itself will retry
                SyncChannelResult.retry()
            } else {
                SyncChannelResult.abort()
            }
        }
        catch (e : InterruptedException) {
            aborted = true
            comCond.signalAll()
            return SyncChannelResult.abort()
        }
        finally {
            ++numExited
            if (numExited == syncSize) {
                // the last one out cleans up
                commitVotes = 0
                aborted = false
                syncValue = SyncChannelResult.none()
                numExited = 0
                exitThroughLobby()
            }
            comLock.unlock()
        }
    }

    fun close() {
        closedLock.lock()
        try {
            closed = true
        } finally {
            closedLock.unlock()
        }
        lobbyLock.lock()
        try {
            lobbyCond.signalAll()
        } finally {
            lobbyLock.unlock()
        }
        comLock.lock()
        try {
            comCond.signalAll()
        } finally {
            comLock.unlock()
        }
    }

    private fun checkIsClosed() : Boolean {
        closedLock.lock()
        try {
            if (closed) {
                return true
            }
        }
        finally {
            closedLock.unlock()
        }
        return false
    }
}

data class SyncChannelResult<V : Any>(
    val isSAT : Boolean,
    val isUNSAT : Boolean,
    val isAborted : Boolean,
    val isRetry : Boolean,
    val result : Optional<V>
) {
    val isEmpty = result.isEmpty
    val isPresent = result.isPresent
    init {
        exspecs.tools.assert(!isPresent || (isSAT && !isUNSAT && !isAborted && !isRetry),
            "Invalid channel result, expected: isPresent => (isSAT && !isUNSAT && !isAborted && !isRetry)")
    }
    companion object {
        fun <V : Any> sat(value : V) : SyncChannelResult<V> {
            return SyncChannelResult(true, false, false, false, Optional.of(value))
        }
        fun <V : Any> unsat() : SyncChannelResult<V> {
            return SyncChannelResult(false, true, false, false, Optional.empty())
        }
        fun <V : Any> abort() : SyncChannelResult<V> {
            return SyncChannelResult(false, false, true, false, Optional.empty())
        }
        fun <V : Any> retry() : SyncChannelResult<V> {
            return SyncChannelResult(false, false, false, true, Optional.empty())
        }
        fun <V : Any> none() : SyncChannelResult<V> {
            return SyncChannelResult(false, false, false, false, Optional.empty())
        }
    }
}
