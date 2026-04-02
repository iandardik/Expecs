package exspecs.ast

import exspecs.program.*
import java.util.*

interface ASTNode {}

class ProgramNode(
    private val procNodes : List<ASTNode>
) : ASTNode {
    fun toTypedAST() : TypedProgramNode {
        val typedProcs = procNodes.map { (it as ProcClassNode).toTypedAST() }
        return TypedProgramNode(typedProcs)
    }
    override fun toString(): String {
        return procNodes.joinToString("\n\n") { it.toString() }
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ASTNode>
) : ASTNode {
    fun toTypedAST() : TypedProcClassNode {
        val varDecls = localDecls.filterIsInstance<VarDeclNode>()
        val actionDecls = localDecls.filterIsInstance<ActionDeclNode>()
        localDecls
            .filter { it !in varDecls && it !in actionDecls }
            .forEach { throw RuntimeException("Unexpected p-class declaration: $it") }
        return TypedProcClassNode(name, varDecls.map { it.toTypedAST() }, actionDecls.map { it.toTypedAST() })
    }
    override fun toString(): String {
        return "p-class $name {${localDecls.joinToString("") { "\n  $it" }}\n}"
    }
}

class VarDeclNode(
    private val name : String,
    private val type : String,
    private val value : ASTNode
) : ASTNode {
    fun toTypedAST() : TypedVarDeclNode {
        if (value !is ExprNode) {
            throw RuntimeException("Expected value of a var decl to be an expression")
        }
        val typeClass = when (type) {
            "Bool" -> BoolType()
            "Int" -> IntType()
            "String" -> StringType()
            else -> InvalidType(type)
        }
        return TypedVarDeclNode(name, typeClass, value.toTypedAST())
    }
    override fun toString(): String {
        return "var $name : $type = $value"
    }
}

class ActionDeclNode(
    private val name : String,
    private val args : ASTNode,
    private val guards : List<ASTNode>,
    private val updates : List<ASTNode>
) : ASTNode {
    fun toTypedAST() : TypedActionDeclNode {
        if (args !is ActionArgsNode) {
            throw RuntimeException("Expected action $name to have an argument list")
        }
        val typedArgs = args.toTypedAST()
        val typedGuards = guards.map { guard ->
            if (guard !is GuardNode) {
                throw RuntimeException("Expected guards in guard decl")
            }
            guard.toTypedAST()
        }
        val typedUpdates = updates.map { update ->
            if (update !is UpdateNode) {
                throw RuntimeException("Expected updates in update decl")
            }
            update.toTypedAST()
        }
        return TypedActionDeclNode(name, typedArgs, typedGuards, typedUpdates)
    }
    override fun toString(): String {
        val body = guards + updates
        return "action $name($args) {${body.joinToString("") { "\n    $it" }}\n  }"
    }
}

class ActionArgsNode(
    private val args : List<ASTNode>
) : ASTNode {
    fun toTypedAST() : TypedActionArgsNode {
        val typedArgs = args.map { arg ->
            if (arg !is ActionArgNode) {
                throw RuntimeException("Expected ActionArgNode")
            }
            arg.toTypedAST()
        }
        return TypedActionArgsNode(typedArgs)
    }
    override fun toString(): String {
        return args.joinToString(", ") { it.toString() }
    }
}

class ActionArgNode(
    private val name : String,
    private val type : String
) : ASTNode {
    fun toTypedAST() : TypedActionArgNode {
        return TypedActionArgNode(name, parseType(type))
    }
    override fun toString(): String {
        return "$name : $type"
    }
}

class GuardNode(
    private val guardExpr : ASTNode
) : ASTNode {
    fun toTypedAST() : TypedGuardNode {
        if (guardExpr !is ExprNode) {
            throw RuntimeException("Expected TypedExprNode")
        }
        val typedGuardExpr = guardExpr.toTypedAST()
        return TypedGuardNode(typedGuardExpr)
    }
    override fun toString(): String {
        return "guard:\n      $guardExpr"
    }
}

class UpdateNode(
    private val updates : List<ASTNode>
) : ASTNode {
    fun toTypedAST() : TypedUpdateNode {
        val typedUpdates = updates.map { update ->
            if (update !is VarUpdateNode) {
                throw RuntimeException("Expected UpdateNode")
            }
            update.toTypedAST()
        }
        return TypedUpdateNode(typedUpdates)
    }
    override fun toString(): String {
        return "update:${updates.joinToString("") { "\n      $it" }}"
    }
}

class VarUpdateNode(
    private val varName : String,
    private val update : ASTNode
) : ASTNode {
    fun toTypedAST() : TypedVarUpdateNode {
        if (update !is ExprNode) {
            throw RuntimeException("Expected ExprNode")
        }
        return TypedVarUpdateNode(varName, update.toTypedAST())
    }
    override fun toString(): String {
        return "$varName := $update"
    }
}

interface ExprNode : ASTNode {
    fun toTypedAST() : TypedExprNode
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ASTNode
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        if (operand !is ExprNode) {
            throw RuntimeException("Expected ExprNode")
        }
        return TypedUnaryOpExprNode(op, operand.toTypedAST())
    }
    override fun toString(): String {
        return "$op $operand"
    }
}

class BinaryOpExprNode(
    private val op : String,
    private val lhsOperand : ASTNode,
    private val rhsOperand : ASTNode
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        if (lhsOperand !is ExprNode || rhsOperand !is ExprNode) {
            throw RuntimeException("Expected ExprNode")
        }
        return TypedBinaryOpExprNode(op, lhsOperand.toTypedAST(), rhsOperand.toTypedAST())
    }
    override fun toString(): String {
        // for readability
        val lhs = if (lhsOperand is BinaryOpExprNode && lhsOperand.op != "=") "($lhsOperand)" else "$lhsOperand"
        val rhs = if (rhsOperand is BinaryOpExprNode && rhsOperand.op != "=") "($rhsOperand)" else "$rhsOperand"
        return "$lhs $op $rhs"
    }
}

class LiteralValueExprNode(
    private val value : String,
    private val type : Type
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        return TypedLiteralValueExprNode(value,type)
    }
    override fun toString(): String {
        return value
    }
}

class SymbolValueExprNode(
    private val symbol : String
) : ExprNode {
    override fun toTypedAST(): TypedExprNode {
        return TypedSymbolValueExprNode(symbol)
    }
    override fun toString(): String {
        return symbol
    }
}
