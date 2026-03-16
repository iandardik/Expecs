import tlc2.tool.Action

class SyncAction(
    private val name : String,
    private val size : Int
) {
    val channel = SyncChannel<Action,Set<Action>>(size) { constraints ->
        println("computing set intersection of:")
        constraints.forEach { println("  $it") }
        val initial = if (constraints.isEmpty()) { emptySet() } else { constraints.first() }
        val sat = constraints.fold(initial) { a,b -> a.intersect(b) }
        println("answer: $sat")
        sat.random()
    }

    fun getName() = name

    override fun equals(other: Any?): Boolean {
        return other is SyncAction && this.name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "SyncAction($name,$size)"
    }
}