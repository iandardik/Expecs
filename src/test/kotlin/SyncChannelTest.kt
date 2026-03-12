import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SyncChannelTest {

    @Test
    fun test1Channel1Sync() {
        businessLogic1Channel(1, 100)
    }

    @Test
    fun test1Channel2Sync() {
        businessLogic1Channel(2, 100)
    }

    @Test
    fun test1Channel3Sync() {
        businessLogic1Channel(3, 99)
    }

    @Test
    fun test1Channel4Sync() {
        businessLogic1Channel(4, 100)
    }

    private fun businessLogic1Channel(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan = SyncChannel(syncSize) { incVal.getAndIncrement() }
        val threads = mutableListOf<Thread>()
        for (i in 1.. numThreads) {
            val t = Thread {
                val syncResult = chan.sync()
                // keep track of how many threads have sync'ed on this value
                results.compute(syncResult.get(), chmResultUpdate)
            }
            t.start()
            threads.add(t)
        }
        threads.forEach { it.join() }

        // testing
        val maxKey = numThreads / syncSize
        assertEquals(results.size, maxKey)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(results[i], syncSize)
        }
    }

    @Test
    fun test2Channels1Sync() {
        businessLogic2Channels(1, 100)
    }

    @Test
    fun test2Channels2Sync() {
        businessLogic2Channels(2, 100)
    }

    @Test
    fun test2Channels3Sync() {
        businessLogic2Channels(3, 99)
    }

    @Test
    fun test2Channels4Sync() {
        businessLogic2Channels(4, 100)
    }

    private fun businessLogic2Channels(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel(syncSize) { incVal.getAndIncrement() }
        val chan2 = SyncChannel(syncSize) { incVal.getAndIncrement() }
        val threads = mutableListOf<Thread>()
        for (i in 1.. numThreads) {
            val t1 = Thread {
                val syncResult = chan1.sync()
                results.compute(syncResult.get(), chmResultUpdate)
            }
            val t2 = Thread {
                val syncResult = chan2.sync()
                results.compute(syncResult.get(), chmResultUpdate)
            }
            t1.start()
            t2.start()
            threads.add(t1)
            threads.add(t2)
        }
        threads.forEach { it.join() }

        // testing
        val maxKey = 2*numThreads / syncSize
        assertEquals(results.size, maxKey)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(results[i], syncSize)
        }
    }

    val chmResultUpdate : (Int,Int?)->Int? = {
            _, curVal ->
        if (curVal == null) {
            1
        } else {
            curVal + 1
        }
    }

    // TODO test cancellation
    /*
    /**
     * This test needs to be fixed to actually test cancellation!
     */
    @Test
    fun testOneChannelCancellation() {
        val randGen = { Random().nextInt() }
        val chan1 = SyncChannel<Int>(2, randGen)
        for (i in 0.. 100) {
            val ffun : (Int)->Boolean = { false }
            val t1 = Thread {
                val rv = chan1.sync().get()
                println("t1: $rv")
            }
            val t2 = Thread {
                val rv = chan1.sync().get()
                println("t2: $rv")
            }
            val t3 = Thread {
                val rv = chan1.sync().get()
                println("t3: $rv")
            }

            val tpool = Executors.newFixedThreadPool(1000)
            tpool.submit(t1)
            tpool.submit(t2)
            tpool.submit(t3)
            tpool.shutdown()
        }
    }
     */
}