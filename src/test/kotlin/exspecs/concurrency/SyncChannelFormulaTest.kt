package exspecs.concurrency

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class SyncChannelFormulaTest {
    private val aliveCheckTimeout = 100L
    private val notAliveCheckTimeout = 1000L

    @Test
    fun testUNSATHangs() {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val (t1,t2) = createUNSATThreads(chan)
        t1.start()
        t2.start()
        t1.join(aliveCheckTimeout)
        t2.join(aliveCheckTimeout)
        assertTrue(t1.isAlive)
        assertTrue(t2.isAlive)
    }

    @Test
    fun testUNSATWithoutRetryModeDoesntHang() {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val (t1,t2) = createUNSATThreads(chan, false)
        t1.start()
        t2.start()
        t1.join(notAliveCheckTimeout)
        t2.join(notAliveCheckTimeout)
        assertTrue(!t1.isAlive)
        assertTrue(!t2.isAlive)
    }

    @Test
    fun testUNSATThenSAT() {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val (t1,t2) = createUNSATThreads(chan)
        t1.start()
        t2.start()
        t1.join(aliveCheckTimeout)
        t2.join(aliveCheckTimeout)
        assertTrue(t1.isAlive)
        assertTrue(t2.isAlive)

        val (t3,t4) = createUNSATThreads(chan)
        t3.start()
        t4.start()
        t3.join(notAliveCheckTimeout)
        t4.join(notAliveCheckTimeout)
        assertTrue(!t1.isAlive)
        assertTrue(!t2.isAlive)
        assertTrue(!t3.isAlive)
        assertTrue(!t4.isAlive)

        //t4.join()
    }

    @Test
    fun testUNSATThenSAT2() {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val (t1,t2) = createUNSATThreads(chan)
        t1.start()
        t2.start()
        t1.join(aliveCheckTimeout)
        t2.join(aliveCheckTimeout)
        assertTrue(t1.isAlive)
        assertTrue(t2.isAlive)

        val (t3,t4) = createUNSATThreads(chan)
        t3.start()
        t3.join(notAliveCheckTimeout)
        assertTrue(!t1.isAlive)
        assertTrue(t2.isAlive)
        assertTrue(!t3.isAlive)

        t4.start()
        t4.join(notAliveCheckTimeout)
        assertTrue(!t2.isAlive)
        assertTrue(!t4.isAlive)
    }

    @Test
    fun testAgainstSpinWaits() {
        val incVal = AtomicInteger(0)
        val chan = createChan(incVal)
        val (t1,t2) = createUNSATThreads(chan)
        t1.start()
        t2.start()
        t1.join(aliveCheckTimeout)
        t2.join(aliveCheckTimeout)

        val (t3,t4) = createUNSATThreads(chan)
        t3.start()
        t4.start()
        t3.join(aliveCheckTimeout)
        t4.join(aliveCheckTimeout)

        // the actual number of times <compute> will be invoked is nondeterministic, but it should be relatively low,
        // e.g., under 15 times.
        assertTrue(incVal.get() <= 15)
    }


    private fun createUNSATThreads(chan : SyncChannel<Int, BoolExpr>, retry : Boolean = true) : List<Thread> {
        val t1 = Thread {
            val ctx = Context()
            chan.sync(ctx.mkLt(ctx.mkIntConst("x"),ctx.mkInt(0)), retryOnUNSAT = retry)
        }
        val t2 = Thread {
            val ctx = Context()
            chan.sync(ctx.mkGt(ctx.mkIntConst("x"),ctx.mkInt(0)), retryOnUNSAT = retry)
        }
        return listOf(t1,t2)
    }

    private fun createChan(incVal : AtomicInteger) : SyncChannel<Int, BoolExpr> {
        val chanCtx = Context()
        val chan = SyncChannel<Int, BoolExpr>(2) { constraints ->
            val i = incVal.getAndIncrement()
            val solver = chanCtx.mkSolver()
            constraints.forEach { c -> solver.add(c.translate(chanCtx)) }
            if (solver.check() == Status.SATISFIABLE) {
                Optional.of(i)
            } else {
                Optional.empty()
            }
        }
        return chan
    }
}