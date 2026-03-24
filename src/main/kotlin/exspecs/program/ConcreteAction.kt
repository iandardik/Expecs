package exspecs.program

// TODO don't hardcode Ints
class ConcreteAction(
    val signature : ActionSignature,
    private val valueMap : Map<String,Int>,
) {
    init {
        signature.args.forEach {
            exspecs.tools.assert(valueMap.containsKey(it), "ConcreteAction missing entry in valueMap")
        }
        valueMap.keys.forEach {
            exspecs.tools.assert(
                signature.args.toSet().contains(it),
                "ConcreteAction has an extraneous entry in valueMap"
            )
        }
    }

    fun hasVar(arg : VarName) : Boolean {
        return valueMap.containsKey(arg.name)
    }

    fun lookupInt(arg : String) : Int {
        return valueMap[arg]!!
    }

    override fun toString() : String {
        return "ConcreteAction($signature): " + signature.args.joinToString(",") { "$it->" + lookupInt(it).toString() }
    }
}