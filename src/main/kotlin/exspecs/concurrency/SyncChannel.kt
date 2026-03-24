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

    private var size = 0
    private var constraints = emptySet<C>()
    private var syncValue = Optional.empty<V>()
    private var commitVotes = 0
    private var aborted = false
    private var numExited = 0
    private var selects : Set<Select> = emptySet()

    fun sync(select : Optional<Select> = Optional.empty()) : Optional<V> {
        return sync(Optional.empty(), select)
    }

    fun sync(constraint : C, select : Optional<Select> = Optional.empty()) : Optional<V> {
        return sync(Optional.of(constraint), select)
    }

    fun sync(constraint : Optional<C>, select : Optional<Select> = Optional.empty()) : Optional<V> {
        var attemptingSync = true
        var syncResult = Optional.empty<V>()
        while (attemptingSync) {
            val enter = enterThroughLobby(constraint, select) // not the fairest policy to have each thread reenter the lobby on each retry
            // TODO this stub below is a bit dangerous, since it doesn't exitThroughLobby() (not always clear when it should)
            if (!enter) {
                // this means that the thread was interrupted, which implies an abort
                return Optional.empty()
            }

            // once we've made it here, attempt to sync
            val (result, value) = syncAttempt(select.isPresent)
            if (result == "abort" || result == "commit") {
                attemptingSync = false
                syncResult = value
            }
        }
        return syncResult
    }

    private fun enterThroughLobby(constraint : Optional<C>, select : Optional<Select>) : Boolean {
        // wait to enter the channel
        try {
            lobbyLock.lock()

            // waiting in the "lobby" to get in
            while (size == syncSize) {
                lobbyCond.await()
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
        try {
            lobbyLock.lock()
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
            allLocks.forEach { it.unlock() }
        }
    }

    private fun syncAttempt(hasSelect : Boolean) : Pair<String,Optional<V>> {
        // the channel has been entered
        try {
            comLock.lock()

            // the first thread to enter this critical section will compute SAT on all formulas
            if (syncValue.isEmpty) {
                syncValue = compute.invoke(constraints)
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
            }
            comCond.signalAll()

            return if (commit && !aborted) {
                Pair("commit", syncValue)
            } else if (commit && hasSelect) {
                // never retry if there's a select--the select itself will retry
                Pair("retry", Optional.empty())
            } else {
                Pair("abort", Optional.empty<V>())
            }
        }
        catch (e : InterruptedException) {
            comCond.signalAll()
            return Pair("abort", Optional.empty<V>())
        }
        finally {
            ++numExited
            if (numExited == syncSize) {
                // the last one out cleans up
                commitVotes = 0
                aborted = false
                syncValue = Optional.empty()
                numExited = 0
                exitThroughLobby()
            }
            comLock.unlock()
        }
    }
}

