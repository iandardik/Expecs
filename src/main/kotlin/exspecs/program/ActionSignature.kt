package exspecs.program

/**
 * Represents the signature to an action, excluding its types (for now).
 * ActionSignatures are not particular to a given transition system / proc.
 */
data class ActionSignature(
    val name : String,
    val args : List<Variable>
) {
    override fun toString(): String {
        return "$name(" + args.joinToString(",") + ")"
    }
}
