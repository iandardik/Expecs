package exspecs.ast

import exspecs.parser.JulayLexer
import exspecs.parser.JulayParser
import exspecs.parser.JulayParserBaseVisitor
import exspecs.program.boolType
import exspecs.program.intType
import exspecs.program.stringType
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream

fun buildAST(input : CharStream) : ASTNode {
    val lexer = JulayLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = JulayParser(tokens)
    return ASTBuilder().visit(parser.program())
}

class ASTBuilder : JulayParserBaseVisitor<ASTNode>() {

    /**
     * A program is a collection of processes.
     */
    override fun visitProgram(ctx: JulayParser.ProgramContext?): ASTNode {
        val procNodes = ctx!!.pclass().map { visit(it) }
        return ProgramNode(procNodes)
    }

    override fun visitPclass(ctx: JulayParser.PclassContext?): ASTNode {
        val name = ctx!!.ID().text
        val localDecls = ctx.pclass_body().map { visit(it) }
        return ProcClassNode(name, localDecls)
    }

    override fun visitPclass_body(ctx: JulayParser.Pclass_bodyContext?): ASTNode {
        return if (ctx!!.var_decl() != null) {
            visit(ctx.var_decl())
        } else if (ctx.action_decl() != null) {
            visit(ctx.action_decl())
        } else {
            throw RuntimeException("Invalid visitPclass_body: no var_decl or action_decl found")
        }
    }

    override fun visitVar_decl(ctx: JulayParser.Var_declContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx.ID(1).text
        val value = visit(ctx.expr())
        return VarDeclNode(name, type, value)
    }

    override fun visitAction_decl(ctx: JulayParser.Action_declContext?): ASTNode {
        val name = ctx!!.ID().text
        val args = visit(ctx.action_args())
        val guards = ctx.guard().map { visit(it) }
        val updates = ctx.update().map { visit(it) }
        return ActionDeclNode(name, args, guards, updates)
    }

    override fun visitAction_args(ctx: JulayParser.Action_argsContext?): ASTNode {
        val args = ctx!!.action_arg().map { visit(it) }
        return ActionArgsNode(args)
    }

    override fun visitAction_arg(ctx: JulayParser.Action_argContext?): ASTNode {
        val name = ctx!!.ID(0).text
        val type = ctx!!.ID(1).text
        return ActionArgNode(name, type)
    }

    override fun visitGuard(ctx: JulayParser.GuardContext?): ASTNode {
        val guardExpr = visit(ctx!!.expr())
        return GuardNode(guardExpr)
    }

    override fun visitUpdate(ctx: JulayParser.UpdateContext?): ASTNode {
        val updates = ctx!!.var_update().map { visit(it) }
        return UpdateNode(updates)
    }

    override fun visitVar_update(ctx: JulayParser.Var_updateContext?): ASTNode {
        val varName = ctx!!.ID().text
        val update = visit(ctx.expr())
        return VarUpdateNode(varName, update)
    }

    override fun visitExpr(ctx: JulayParser.ExprContext?): ASTNode {
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

    override fun visitValue(ctx: JulayParser.ValueContext?): ASTNode {
        return if (ctx!!.ID() != null) {
            SymbolValueExprNode(ctx.ID().text)
        } else if (ctx.INT() != null) {
            LiteralValueExprNode(ctx.INT().text, intType)
        } else if (ctx.TRUE() != null) {
            LiteralValueExprNode(ctx.TRUE().text, boolType)
        } else if (ctx.FALSE() != null) {
            LiteralValueExprNode(ctx.FALSE().text, boolType)
        } else if (ctx.STRING() != null) {
            val rawStr = ctx.STRING().text
            val unquotedStr = rawStr.substring(1,rawStr.length-1)
            LiteralValueExprNode(unquotedStr, stringType)
        } else {
            throw RuntimeException("Invalid visitValue: invalid expression found: ${ctx.text}")
        }
    }
}