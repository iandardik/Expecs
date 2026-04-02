package exspecs.ast

import com.microsoft.z3.*
import exspecs.program.*
import exspecs.tools.mkStringConst

// TODO there is a bug with having multiple procs with the same local var name

interface TypedNode {}

interface TypedExprNode : TypedNode {
    fun toZ3Expr(ctx : Context, symbolTypeTable : Map<String,Type>) : Expr<*>
    fun toProgramExpr(symbolTypeTable : Map<String,Type>) : ProgramExpr
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
    private val symbolTypeTable = mutableMapOf<String,Type>()

    fun toTransitionSystem() : TransitionSystem {
        val initStateAssignments = varDecls
            .map { it.toAssignment() }
            .associate { it }
        val initState = State(initStateAssignments)

        val ctx = Context()
        val symbolTypeTable = initStateAssignments.keys.associate { Pair(it.name,it.type) }
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
    private val type : Type,
    private val initExpr : TypedExprNode
) : TypedNode {
    fun toAssignment() : Pair<Variable,Value> {
        val variable = Variable(name,type)
        // precompute the value statically, since it will be known at "compile" time
        val precomputedValue = initExpr.toProgramExpr(emptyMap()).eval(emptyState(), emptyConcreteAction())
        val value = Value(precomputedValue.value,type)
        return Pair(variable,value)
    }
    override fun toString(): String {
        return "var $name : $type = $initExpr"
    }
}

class TypedActionDeclNode(
    private val name : String,
    private val args : TypedActionArgsNode,
    private val guards : List<TypedGuardNode>,
    private val updates : List<TypedUpdateNode>,
) : TypedNode {
    fun toSymbolicAction(ctx : Context, symbolTypeTable : Map<String,Type>) : SymbolicAction {
        val sig = ActionSignature(name, args.toVariables())
        // create a new symbol table with the action arguments (action args have a stronger scope than state vars)
        val symbolTypeTableWithActs = symbolTypeTable + (sig.args.map { Pair(it.name,it.type) }.associate { it })

        val guard = guards.fold(ctx.mkTrue()) { acc,g ->
            val gExpr = g.toZ3Expr(ctx,symbolTypeTableWithActs) as BoolExpr
            ctx.mkAnd(acc,gExpr)
        }
        val varUpdates = updates // TODO need to make sure variables are not updated multiple times
            .fold(emptyMap<Variable,ProgramExpr>()) { acc,u -> acc + u.toStateVarUpdate(symbolTypeTableWithActs) }
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
    fun toVariables() : List<Variable> {
        return args.map { it.toVariable() }
    }
    override fun toString(): String {
        return args.joinToString(", ") { it.toString() }
    }
}

class TypedActionArgNode(
    private val name : String,
    private val type : Type
) : TypedNode {
    fun toVariable() : Variable {
        return Variable(name, type)
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class TypedGuardNode(
    private val guardExpr : TypedExprNode
) : TypedNode {
    fun toZ3Expr(ctx : Context, symbolTypeTable : Map<String,Type>) : Expr<*> {
        return guardExpr.toZ3Expr(ctx,symbolTypeTable)
    }
    override fun toString(): String {
        return "guard:\n      $guardExpr"
    }
}

class TypedUpdateNode(
    private val updates : List<TypedVarUpdateNode>
) : TypedNode {
    fun toStateVarUpdate(symbolTypeTable : Map<String,Type>) : Map<Variable,ProgramExpr> {
        return updates.associate { it.toStateVarUpdate(symbolTypeTable) }
    }
    override fun toString(): String {
        return "update:${updates.joinToString("") { "\n      $it" }}"
    }
}

class TypedVarUpdateNode(
    private val varName : String,
    private val update : TypedExprNode
) : TypedNode {
    fun toStateVarUpdate(symbolTypeTable : Map<String,Type>) : Pair<Variable,ProgramExpr> {
        return Pair(Variable(varName,symbolTypeTable[varName]!!), update.toProgramExpr(symbolTypeTable))
    }
    override fun toString(): String {
        return "$varName := $update"
    }
}

class TypedUnaryOpExprNode(
    private val op : String,
    private val operand : TypedExprNode
) : TypedExprNode {
    override fun toZ3Expr(ctx: Context, symbolTypeTable: Map<String, Type>): Expr<*> {
        return when (op) {
            "~" -> ctx.mkNot(operand.toZ3Expr(ctx,symbolTypeTable) as BoolExpr)
            else -> throw RuntimeException("Invalid unary op: $op")
        }
    }

    override fun toProgramExpr(symbolTypeTable: Map<String, Type>): ProgramExpr {
        return when (op) {
            "~" -> NotProgramExpr(operand.toProgramExpr(symbolTypeTable))
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
    override fun toZ3Expr(ctx: Context, symbolTypeTable : Map<String,Type>) : Expr<*> {
        return when (op) {
            "=" -> ctx.mkEq(lhsOperand.toZ3Expr(ctx,symbolTypeTable), rhsOperand.toZ3Expr(ctx,symbolTypeTable))
            "<" -> ctx.mkLt(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr)
            "<=" -> ctx.mkLe(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr)
            ">" -> ctx.mkGt(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr)
            ">=" -> ctx.mkGe(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr)
            "&" -> ctx.mkAnd(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as BoolExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as BoolExpr)
            "|" -> ctx.mkOr(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as BoolExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as BoolExpr)
            "+" -> ctx.mkAdd(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr)
            "-" -> ctx.mkSub(lhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr, rhsOperand.toZ3Expr(ctx,symbolTypeTable) as ArithExpr)
            else -> throw RuntimeException("Invalid binary op: $op")
        }
    }

    override fun toProgramExpr(symbolTypeTable: Map<String, Type>): ProgramExpr {
        return when (op) {
            "+" -> PlusProgramExpr(lhsOperand.toProgramExpr(symbolTypeTable), rhsOperand.toProgramExpr(symbolTypeTable))
            "-" -> MinusProgramExpr(lhsOperand.toProgramExpr(symbolTypeTable), rhsOperand.toProgramExpr(symbolTypeTable))
            "=" -> EqProgramExpr(lhsOperand.toProgramExpr(symbolTypeTable), rhsOperand.toProgramExpr(symbolTypeTable))
            else -> throw RuntimeException("Unsupported or invalid binary op: $op")
        }
    }

    override fun toString(): String {
        // for readability
        val lhs = if (lhsOperand is TypedBinaryOpExprNode && lhsOperand.op != "=") "($lhsOperand)" else "$lhsOperand"
        val rhs = if (rhsOperand is TypedBinaryOpExprNode && rhsOperand.op != "=") "($rhsOperand)" else "$rhsOperand"
        return "$lhs $op $rhs"
    }
}

class TypedLiteralValueExprNode(
    private val value : String,
    private val type : Type
) : TypedExprNode {
    override fun toZ3Expr(ctx: Context, symbolTypeTable : Map<String,Type>) : Expr<*> {
        return when (type) {
            is IntType -> ctx.mkInt(Integer.parseInt(value))
            is BoolType -> ctx.mkBool(value.lowercase() == "true")
            is StringType -> ctx.mkString(value)
            else -> throw RuntimeException("Unexpected type: $type")
        }
    }

    override fun toProgramExpr(symbolTypeTable: Map<String,Type>) : ProgramExpr {
        return when (type) {
            is BoolType -> BoolLiteralProgramExpr(value.lowercase() == "true")
            is IntType -> IntLiteralProgramExpr(Integer.parseInt(value))
            is StringType -> StringLiteralProgramExpr(value)
            else -> throw RuntimeException("Unexpected type: $type")
        }
    }

    override fun toString(): String {
        return value
    }
}

class TypedSymbolValueExprNode(
    private val name : String
) : TypedExprNode {
    override fun toZ3Expr(ctx: Context, symbolTypeTable : Map<String,Type>) : Expr<*> {
        return when (val type = symbolTypeTable[name]) {
            is IntType -> ctx.mkIntConst(name)
            is BoolType -> ctx.mkBoolConst(name)
            is StringType -> ctx.mkStringConst(name)
            else -> throw RuntimeException("Unexpected type: $type")
        }
    }

    override fun toProgramExpr(symbolTypeTable: Map<String,Type>) : ProgramExpr {
        return SymbolProgramExpr(Variable(name,symbolTypeTable[name]!!))
    }

    override fun toString(): String {
        return name
    }
}
