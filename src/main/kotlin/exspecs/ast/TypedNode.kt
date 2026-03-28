package exspecs.ast

import com.microsoft.z3.*
import exspecs.program.*
import exspecs.tools.mkStringConst

interface TypedNode {}

interface TypedExprNode : TypedNode {
    fun toZ3BoolExpr(ctx : Context, symbolTypeTable : MutableMap<String,String>) : Expr<out Sort?>
    fun <T> toUpdateExpr(symbolTypeTable : MutableMap<String,String>) : UpdateExpr<T>
    fun getType() : String
    // TODO these methods below should probably (instead) return some sort of tree to evaluate the expression
    fun evalToInt() : Int
    fun evalToString() : String
    fun evalToBool() : Boolean
}

class TypedProgramNode(
    private val procNodes : List<TypedProcClassNode>
) : TypedNode {
    fun toProgram() : Program {
        val transitionSystems = procNodes.map { it.toTransitionSystem() }.toSet()
        return Program(transitionSystems)
    }
    override fun toString(): String {
        return procNodes.joinToString("\n\n") { it.toString() }
    }
}

class TypedProcClassNode(
    private val name : String,
    private val varDecls : List<TypedVarDeclNode>,
    private val actionDecls : List<TypedActionDeclNode>
) : TypedNode {
    private val symbolTypeTable = mutableMapOf<String,String>()

    fun toTransitionSystem() : TransitionSystem {
        val initStateAssignments = varDecls.map { it.toAssignment(symbolTypeTable) }.toSet()
        val initState = State(initStateAssignments)

        val ctx = Context()
        val alphabet = actionDecls.map { it.toSymbolicAction(ctx,symbolTypeTable) }.toSet()
        return GenericTransitionSystem(initState, alphabet, name, ctx)
    }
    override fun toString(): String {
        val localDecls = varDecls + actionDecls
        return "p-class Typed$name {${localDecls.joinToString("") { "\n  $it" }}\n}"
    }
}

class TypedVarDeclNode(
    private val name : String,
    private val type : String,
    private val expr : TypedExprNode
) : TypedNode {
    fun toAssignment(symbolTypeTable : MutableMap<String,String>) : VarAssignment {
        symbolTypeTable[name] = type
        return when (type) {
            "Int" -> {
                val intVal = expr.evalToInt()
                IntVarAssignment(Variable(name,type), intVal)
            }
            "String" -> {
                val strVal = expr.evalToString()
                StringVarAssignment(Variable(name,type), strVal)
            }
            else -> throw RuntimeException("Encountered unknown type: $type")
        }
    }
    override fun toString(): String {
        return "var $name : $type = $expr"
    }
}

class TypedActionDeclNode(
    private val name : String,
    private val args : TypedActionArgsNode,
    private val guards : List<TypedGuardNode>,
    private val updates : List<TypedUpdateNode>,
) : TypedNode {
    fun toSymbolicAction(ctx : Context, symbolTypeTable : MutableMap<String,String>) : SymbolicAction {
        val sig = ActionSignature(name, args.toVariables(symbolTypeTable))
        val guard = guards.fold(ctx.mkTrue()) { acc,g ->
            val gExpr = g.toZ3BoolExpr(ctx,symbolTypeTable) as BoolExpr
            ctx.mkAnd(acc,gExpr)
        }
        val varUpdates = updates.flatMap { it.toStateVarUpdate(symbolTypeTable) }.toSet()
        return SymbolicAction(sig, guard, varUpdates)
    }
    override fun toString(): String {
        val body = guards + updates
        return "action $name($args) {${body.joinToString("") { "\n    $it" }}\n  }"
    }
}

class TypedActionArgsNode(
    private val args : List<TypedActionArgNode>
) : TypedNode {
    fun toVariables(symbolTypeTable : MutableMap<String,String>) : List<Variable> {
        return args.map { it.toVariable(symbolTypeTable) }
    }
    override fun toString(): String {
        return args.joinToString(", ") { it.toString() }
    }
}

class TypedActionArgNode(
    private val name : String,
    private val type : String
) : TypedNode {
    fun toVariable(symbolTypeTable : MutableMap<String,String>) : Variable {
        symbolTypeTable[name] = type
        return Variable(name, type)
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class TypedGuardNode(
    private val guardExpr : TypedExprNode
) : TypedNode {
    fun toZ3BoolExpr(ctx : Context, symbolTypeTable : MutableMap<String,String>) : Expr<out Sort?> {
        return guardExpr.toZ3BoolExpr(ctx,symbolTypeTable)
    }
    override fun toString(): String {
        return "guard:\n      $guardExpr"
    }
}

class TypedUpdateNode(
    private val updates : List<TypedVarUpdateNode>
) : TypedNode {
    fun toStateVarUpdate(symbolTypeTable : MutableMap<String,String>) : Set<StateVarUpdate> {
        return updates.map { it.toStateVarUpdate(symbolTypeTable) }.toSet()
    }
    override fun toString(): String {
        return "update:${updates.joinToString("") { "\n      $it" }}"
    }
}

class TypedVarUpdateNode(
    private val varName : String,
    private val update : TypedExprNode
) : TypedNode {
    fun toStateVarUpdate(symbolTypeTable : MutableMap<String,String>) : StateVarUpdate {
        return when (val type = update.getType()) {
            "Int" -> IntStateVarUpdate(Variable(varName,type), update.toUpdateExpr(symbolTypeTable))
            "String" -> StringStateVarUpdate(Variable(varName,type), update.toUpdateExpr(symbolTypeTable))
            "Symbol" -> {
                return when (symbolTypeTable[varName]) {
                    "Int" -> IntStateVarUpdate(Variable(varName,symbolTypeTable[varName]!!), update.toUpdateExpr(symbolTypeTable))
                    "String" -> StringStateVarUpdate(Variable(varName,symbolTypeTable[varName]!!), update.toUpdateExpr(symbolTypeTable))
                    else -> throw RuntimeException("Unexpected type '${symbolTypeTable[varName]}' for update: $varName := $update")
                }
            }
            else -> throw RuntimeException("Unexpected type '$type' for update: $varName := $update")
        }
    }
    override fun toString(): String {
        return "$varName := $update"
    }
}

class TypedUnaryOpExprNode(
    private val op : String,
    private val operand : TypedExprNode
) : TypedExprNode {
    override fun toZ3BoolExpr(ctx: Context, symbolTypeTable : MutableMap<String,String>) : Expr<out Sort?> {
        return when (op) {
            "~" -> ctx.mkNot(operand.toZ3BoolExpr(ctx,symbolTypeTable) as BoolExpr)
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun <T> toUpdateExpr(symbolTypeTable : MutableMap<String,String>): UpdateExpr<T> {
        return when (op) {
            "~" -> NotUpdateExpr(operand.toUpdateExpr(symbolTypeTable)) as UpdateExpr<T>
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun getType(): String {
        return when (op) {
            "~" -> "Bool"
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun evalToInt(): Int {
        return when (op) {
            "~" -> throw RuntimeException("Not an Int")
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun evalToString(): String {
        return when (op) {
            "~" -> throw RuntimeException("Not a String")
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun evalToBool(): Boolean {
        return when (op) {
            "~" -> !operand.evalToBool()
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun toString(): String {
        return "$op $operand"
    }
}

class TypedBinaryOpExprNode(
    private val op : String,
    private val lhsOperand : TypedExprNode,
    private val rhsOperand : TypedExprNode
) : TypedExprNode {
    override fun toZ3BoolExpr(ctx: Context, symbolTypeTable : MutableMap<String,String>) : Expr<out Sort?> {
        return when (op) {
            "=" -> ctx.mkEq(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable), rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable))
            "<" -> ctx.mkLt(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr)
            "<=" -> ctx.mkLe(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr)
            ">" -> ctx.mkGt(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr)
            ">=" -> ctx.mkGe(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr)
            "&" -> ctx.mkAnd(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as BoolExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as BoolExpr)
            "|" -> ctx.mkOr(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as BoolExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as BoolExpr)
            "+" -> ctx.mkAdd(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr)
            "-" -> ctx.mkSub(lhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3BoolExpr(ctx,symbolTypeTable) as ArithExpr)
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }

    override fun <T> toUpdateExpr(symbolTypeTable : MutableMap<String,String>): UpdateExpr<T> {
        return when (op) {
            "+" -> PlusIntUpdateExpr(lhsOperand.toUpdateExpr(symbolTypeTable), rhsOperand.toUpdateExpr(symbolTypeTable)) as UpdateExpr<T>
            "-" -> MinusIntUpdateExpr(lhsOperand.toUpdateExpr(symbolTypeTable), rhsOperand.toUpdateExpr(symbolTypeTable)) as UpdateExpr<T>
            else -> throw RuntimeException("Unsupported or invalid binary op: $op")
        }
    }

    override fun getType(): String {
        return when (op) {
            "=" -> "Bool"
            "<" -> "Bool"
            "<=" -> "Bool"
            ">" -> "Bool"
            ">=" -> "Bool"
            "&" -> "Bool"
            "|" -> "Bool"
            "+" -> "Int"
            "-" -> "Int"
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }

    override fun evalToInt(): Int {
        return when (op) {
            "+" -> lhsOperand.evalToInt() + rhsOperand.evalToInt()
            "-" -> lhsOperand.evalToInt() - rhsOperand.evalToInt()
            else -> throw RuntimeException("Unsupported or invalid binary op: $op")
        }
    }

    override fun evalToString(): String {
        throw RuntimeException("Not supported")
    }

    override fun evalToBool(): Boolean {
        throw RuntimeException("Not supported")
    }

    override fun toString(): String {
        // for readability
        val lhs = if (lhsOperand is TypedBinaryOpExprNode && lhsOperand.op != "=") "($lhsOperand)" else "$lhsOperand"
        val rhs = if (rhsOperand is TypedBinaryOpExprNode && rhsOperand.op != "=") "($rhsOperand)" else "$rhsOperand"
        return "$lhs $op $rhs"
    }
}

class TypedValueExprNode(
    private val value : String,
    private val type : String
) : TypedExprNode {
    override fun toZ3BoolExpr(ctx: Context, symbolTypeTable : MutableMap<String,String>) : Expr<out Sort?> {
        return when (type) {
            "Int" -> ctx.mkInt(Integer.parseInt(value))
            "String" -> ctx.mkString(value)
            "Symbol" -> when (symbolTypeTable[value]) {
                "Int" -> ctx.mkIntConst(value)
                "String" -> ctx.mkStringConst(value)
                else -> throw RuntimeException("Unsupported symbol type: ${symbolTypeTable[value]}")
            }
            else -> throw RuntimeException("Unexpected type: $type")
        }
    }

    override fun <T> toUpdateExpr(symbolTypeTable : MutableMap<String,String>): UpdateExpr<T> {
        return when (type) {
            // TODO these casts are ugly
            "Int" -> IntUpdateExpr(Integer.parseInt(value)) as UpdateExpr<T>
            "String" -> StringUpdateExpr(value) as UpdateExpr<T>
            "Symbol" -> when (symbolTypeTable[value]) {
                "Int" -> IntVarUpdateExpr(Variable(value,"Int")) as UpdateExpr<T>
                "String" -> StringVarUpdateExpr(Variable(value,"String")) as UpdateExpr<T>
                else -> throw RuntimeException("Unexpected symbol type: $type")
            }
            else -> throw RuntimeException("Unexpected type: $type")
        }
    }

    override fun getType(): String {
        return type
    }

    override fun evalToInt(): Int {
        if (type != "Int") {
            throw RuntimeException("Not an Int ($value : $type)")
        }
        return Integer.parseInt(value)
    }

    override fun evalToString(): String {
        if (type != "String") {
            throw RuntimeException("Not a String ($value : $type)")
        }
        return value
    }

    override fun evalToBool(): Boolean {
        if (type != "Bool") {
            throw RuntimeException("Not a Bool ($value : $type)")
        }
        // TODO sanity check for the value
        return value == "true"
    }

    override fun toString(): String {
        return value
    }
}
