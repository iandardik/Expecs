package exspecs.ast

import exspecs.parser.ExspecLexer
import exspecs.parser.ExspecParser
import exspecs.parser.ExspecParserBaseVisitor
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream

fun buildAST(input : CharStream) : ASTNode {
    val lexer = ExspecLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = ExspecParser(tokens)
    return ASTBuilder().visit(parser.program())
}

class ASTBuilder : ExspecParserBaseVisitor<ASTNode>() {

    /**
     * A program is a collection of processes.
     */
    override fun visitProgram(ctx: ExspecParser.ProgramContext?): ASTNode {
        val procNodes = ctx!!.pclass().map { visit(it) }
        return ProgramNode(procNodes)
    }

    override fun visitPclass(ctx: ExspecParser.PclassContext?): ASTNode {
        val name = ctx!!.ID().text
        val localDecls = ctx.pclass_body().map { visit(it) }
        return ProcClassNode(name, localDecls)
    }

    override fun visitPclass_body(ctx: ExspecParser.Pclass_bodyContext?): ASTNode {
        return if (ctx!!.var_decl() != null) {
            visit(ctx.var_decl())
        } else if (ctx.action_decl() != null) {
            visit(ctx.action_decl())
        } else {
            throw RuntimeException("Invalid visitPclass_body: no var_decl or action_decl found")
        }
    }

    override fun visitVar_decl(ctx: ExspecParser.Var_declContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx.ID(1).text
        val value = visit(ctx.expr())
        return VarDeclNode(name, type, value)
    }

    override fun visitAction_decl(ctx: ExspecParser.Action_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.action_args())
        val guards = ctx.guard().map { visit(it) }
        val updates = ctx.update().map { visit(it) }
        return ActionDeclNode(name, args, guards, updates)
    }

    override fun visitAction_args(ctx: ExspecParser.Action_argsContext?): ASTNode {
        val args = ctx!!.action_arg().map { visit(it) }
        return ActionArgsNode(args)
    }

    override fun visitAction_arg(ctx: ExspecParser.Action_argContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx!!.ID(1).text
        return ActionArgNode(name, type)
    }

    override fun visitGuard(ctx: ExspecParser.GuardContext?): ASTNode {
        val guardExpr = visit(ctx!!.expr())
        return GuardNode(guardExpr)
    }

    override fun visitUpdate(ctx: ExspecParser.UpdateContext?): ASTNode {
        val updates = ctx!!.var_update().map { visit(it) }
        return UpdateNode(updates)
    }

    override fun visitVar_update(ctx: ExspecParser.Var_updateContext?): ASTNode {
        val varName = ctx!!.ID().text
        val update = visit(ctx.expr())
        return VarUpdateNode(varName, update)
    }

    override fun visitExpr(ctx: ExspecParser.ExprContext?): ASTNode {
       return if (ctx!!.EQ() != null) {
           BinaryOpExprNode("=", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.LT() != null) {
           BinaryOpExprNode("<", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.LTE() != null) {
           BinaryOpExprNode("<=", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.GT() != null) {
           BinaryOpExprNode(">", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.GTE() != null) {
           BinaryOpExprNode(">=", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.AND() != null) {
           BinaryOpExprNode("&", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.OR() != null) {
           BinaryOpExprNode("|", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.NOT() != null) {
           UnaryOpExprNode("~", visit(ctx.expr(0)))
       } else if (ctx.PLUS() != null) {
           BinaryOpExprNode("+", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.MINUS() != null) {
           BinaryOpExprNode("-", visit(ctx.expr(0)), visit(ctx.expr(1)))
       } else if (ctx.LPAREN() != null) {
           visit(ctx.expr(0))
       } else if (ctx.value() != null) {
           visit(ctx.value())
       } else {
           throw RuntimeException("Invalid visitExpr: invalid expression found: ${ctx.text}")
       }
    }

    override fun visitValue(ctx: ExspecParser.ValueContext?): ASTNode {
        return if (ctx!!.ID() != null) {
            ValueExprNode(ctx.ID().text, "Symbol")
        } else if (ctx.INT() != null) {
            ValueExprNode(ctx.INT().text, "Int")
        } else if (ctx.TRUE() != null) {
            ValueExprNode(ctx.TRUE().text, "Bool")
        } else if (ctx.FALSE() != null) {
            ValueExprNode(ctx.FALSE().text, "Bool")
        } else if (ctx.STRING() != null) {
            ValueExprNode(ctx.STRING().text, "String")
        } else {
            throw RuntimeException("Invalid visitValue: invalid expression found: ${ctx.text}")
        }
    }
}