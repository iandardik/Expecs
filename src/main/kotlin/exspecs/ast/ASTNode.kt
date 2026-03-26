package exspecs.ast

import exspecs.program.Proc
import java.util.*

interface ASTNode {}

class ProgramNode(
    private val procNodes : List<ASTNode>
) : ASTNode {
    override fun toString(): String {
        return procNodes.joinToString("\n\n") { it.toString() }
    }
}

class ProcClassNode(
    private val name : String,
    private val localDecls : List<ASTNode>
) : ASTNode {
    fun getName() = name
    override fun toString(): String {
        return "p-class $name {${localDecls.joinToString("") { "\n  $it" }}\n}"
    }
}

class VarDeclNode(
    private val name : String,
    private val type : String,
    private val value : ASTNode
) : ASTNode {
    override fun toString(): String {
        return "var $name : $type = $value"
    }
}

class ActionDeclNode(
    private val name : String,
    private val args : ASTNode,
    private val body : List<ASTNode>
) : ASTNode {
    override fun toString(): String {
        return "action $name($args) {${body.joinToString("") { "\n    $it" }}\n  }"
    }
}

class ActionArgsNode(
    private val args : List<ASTNode>
) : ASTNode {
    override fun toString(): String {
        return args.joinToString(", ") { it.toString() }
    }
}

class ActionArgNode(
    private val name : String,
    private val type : String
) : ASTNode {
    override fun toString(): String {
        return "$name : $type"
    }
}

class GuardNode(
    private val guardExpr : ASTNode
) : ASTNode {
    override fun toString(): String {
        return "guard:\n      $guardExpr"
    }
}

class UpdateNode(
    private val updates : List<ASTNode>
) : ASTNode {
    override fun toString(): String {
        return "update:${updates.joinToString("") { "\n      $it" }}"
    }
}

class VarUpdateNode(
    private val varName : String,
    private val update : ASTNode
) : ASTNode {
    override fun toString(): String {
        return "$varName := $update"
    }
}

class UnaryOpExprNode(
    private val op : String,
    private val operand : ASTNode
) : ASTNode {
    override fun toString(): String {
        return "$op $operand"
    }
}

class BinaryOpExprNode(
    private val op : String,
    private val lhsOperand : ASTNode,
    private val rhsOperand : ASTNode
) : ASTNode {
    override fun toString(): String {
        // for readability
        val lhs = if (lhsOperand is BinaryOpExprNode && lhsOperand.op != "=") "($lhsOperand)" else "$lhsOperand"
        val rhs = if (rhsOperand is BinaryOpExprNode && rhsOperand.op != "=") "($rhsOperand)" else "$rhsOperand"
        return "$lhs $op $rhs"
    }
}

class IDValueNode(
    private val name : String
) : ASTNode {
    override fun toString(): String {
        return name
    }
}

class IntValueNode(
    private val value : Int
) : ASTNode {
    override fun toString(): String {
        return "$value"
    }
}

class BoolValueNode(
    private val value : Boolean
) : ASTNode {
    override fun toString(): String {
        return "$value"
    }
}

class StringValueNode(
    private val value : String
) : ASTNode {
    override fun toString(): String {
        return "$value"
    }
}
