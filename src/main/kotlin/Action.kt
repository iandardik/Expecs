class Action(private val name : String) {
    private val procs : MutableSet<Proc> = mutableSetOf()

    fun addProc(p : Proc) {
        procs.add(p)
    }

    fun mustSync() : Boolean {
        assert(procs.size in 1..2)
        return procs.size > 1
    }

    fun mustSyncWith(p : Proc) : Proc {
        assert(procs.size in 1..2)
        val others = procs.filter { it != p }
        assert(others.size == 1)
        return others[0]
    }

    override fun toString(): String {
        return name
    }
}