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
    private val lobbyLock = ReentrantLock()
    private val lobbyCond = lobbyLock.newCondition()
    private val comLock = ReentrantLock()
    private val comCond = comLock.newCondition()
    private val closedLock = ReentrantLock()

    private var size = 0
    private var constraints = emptySet<C>()
    private var syncValue = SyncChannelResult.none<V>()
    private var commitVotes = 0
    private var aborted = false
    private var numExited = 0
    private var selects : Set<Select> = emptySet()
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
        var attemptingSync = true
        var syncResult = SyncChannelResult.none<V>()
        while (attemptingSync) {
            val enter = enterThroughLobby(constraint, select, retryOnUNSAT) // not the fairest policy to have each thread reenter the lobby on each retry
            // TODO this stub below is a bit dangerous, since it doesn't exitThroughLobby() (not always clear when it should)
            if (!enter) {
                // this means that the thread was interrupted, which implies an abort
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

            // TODO if the syncValue is UNSAT then we should retry

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
