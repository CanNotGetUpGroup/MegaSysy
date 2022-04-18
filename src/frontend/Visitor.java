package frontend;

import ir.Module;
import ir.MyContext;
import ir.Value;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class Visitor extends SysyBaseVisitor<Value> {
    static MyIRBuilder builder;
    MyContext context = MyContext.getInstance();
    Module module = Module.getInstance();
    Value curVal;
    String type;
    Logger logger = Logger.getLogger(Visitor.class.getName());
    Value LogError(String str){
        logger.severe(str);
        return null;
    }

    @Override
    public Value visitProgram(SysyParser.ProgramContext ctx) {

        return super.visitProgram(ctx);
    }

    /**
     * compUnit : compUnit(decl|funcDef)|(decl|funcDef);
     */
    @Override
    public Value visitCompUnit(SysyParser.CompUnitContext ctx) {
        return super.visitCompUnit(ctx);
    }

    /**
     * decl : constDecl | varDecl ;
     */
    @Override
    public Value visitDecl(SysyParser.DeclContext ctx) {
        return super.visitDecl(ctx);
    }

    /**
     * constDecl : CONST bType constDef ( COMMA constDef )* SEMICOLON;
     */
    @Override
    public Value visitConstDecl(SysyParser.ConstDeclContext ctx) {
        type = ctx.bType().getText();
        if(type.equals("int")){

        }else{

        }
        return super.visitConstDecl(ctx);
    }

    /**
     * bType : INT | FLOAT;
     */
    @Override
    public Value visitBType(SysyParser.BTypeContext ctx) {
        return super.visitBType(ctx);
    }

    /**
     * constDef : IDENT (LBRACKET constExp RBRACKET )* EQ constInitVal;
     */
    @Override
    public Value visitConstDef(SysyParser.ConstDefContext ctx) {
        String name=ctx.IDENT().getText();
        return super.visitConstDef(ctx);
    }

    /**
     * constInitVal : constExp | LBRACE(constInitVal (COMMA constInitVal)*)?RBRACE;
     */
    @Override
    public Value visitConstInitVal(SysyParser.ConstInitValContext ctx) {
        return super.visitConstInitVal(ctx);
    }

    /**
     * constExp : addExp ;
     */
    @Override
    public Value visitConstExp(SysyParser.ConstExpContext ctx) {
        return super.visitConstExp(ctx);
    }

    /**
     * varDecl : bType varDef ( COMMA varDef )* SEMICOLON;
     */
    @Override
    public Value visitVarDecl(SysyParser.VarDeclContext ctx) {
        return super.visitVarDecl(ctx);
    }

    /**
     * varDef : IDENT (LBRACKET constExp RBRACKET )* | IDENT (LBRACKET constExp RBRACKET)*  EQ initVal;
     */
    @Override
    public Value visitVarDef(SysyParser.VarDefContext ctx) {
        return super.visitVarDef(ctx);
    }

    /**
     * initVal : exp | LBRACE (initVal (COMMA initVal)*)? RBRACE;
     */
    @Override
    public Value visitInitVal(SysyParser.InitValContext ctx) {
        return super.visitInitVal(ctx);
    }

    /**
     * funcDef : funcType IDENT LPAREN (funcFParams)? RPAREN  block;
     */
    @Override
    public Value visitFuncDef(SysyParser.FuncDefContext ctx) {
        return super.visitFuncDef(ctx);
    }

    /**
     * funcType : INT|VOID|FLOAT;
     */
    @Override
    public Value visitFuncType(SysyParser.FuncTypeContext ctx) {
        return super.visitFuncType(ctx);
    }

    /**
     * funcFParams : funcFParam ( COMMA funcFParam )*;
     */
    @Override
    public Value visitFuncFParams(SysyParser.FuncFParamsContext ctx) {
        return super.visitFuncFParams(ctx);
    }

    /**
     * funcFParam : bType IDENT  (LBRACKET RBRACKET ( LBRACKET exp RBRACKET )*)?;
     */
    @Override
    public Value visitFuncFParam(SysyParser.FuncFParamContext ctx) {
        return super.visitFuncFParam(ctx);
    }

    /**
     * block : LBRACE ( blockItem )* RBRACE;
     */
    @Override
    public Value visitBlock(SysyParser.BlockContext ctx) {
        return super.visitBlock(ctx);
    }

    /**
     * blockItem: decl | stmt;
     */
    @Override
    public Value visitBlockItem(SysyParser.BlockItemContext ctx) {
        return super.visitBlockItem(ctx);
    }

    /**
     * stmt:lVal EQ exp SEMICOLON
     *     | block
     *     | (exp)? SEMICOLON
     *     | IF LPAREN cond RPAREN stmt ( ELSE stmt)?
     *     | WHILE LPAREN  cond RPAREN  stmt
     *     | BREAK SEMICOLON
     *     | CONTINUE SEMICOLON
     *     | RETURN (exp)? SEMICOLON ;
     */
    @Override
    public Value visitStmt(SysyParser.StmtContext ctx) {
        return super.visitStmt(ctx);
    }

    /**
     * exp : addExp;
     */
    @Override
    public Value visitExp(SysyParser.ExpContext ctx) {
        return super.visitExp(ctx);
    }

    /**
     * cond : lOrExp;
     */
    @Override
    public Value visitCond(SysyParser.CondContext ctx) {
        return super.visitCond(ctx);
    }

    /**
     * lVal : IDENT (LBRACKET exp RBRACKET )*;
     */
    @Override
    public Value visitLVal(SysyParser.LValContext ctx) {
        String name=ctx.IDENT().getText();
        curVal = context.lookup(name);
        if(curVal==null){
            return LogError("未找到名为: "+name+" 的变量");
        }

        return super.visitLVal(ctx);
    }

    /**
     * primaryExp : LPAREN exp RPAREN | lVal | number ;
     */
    @Override
    public Value visitPrimaryExp(SysyParser.PrimaryExpContext ctx) {
        return super.visitPrimaryExp(ctx);
    }

    /**
     * unaryExp  : primaryExp | IDENT LPAREN(funcRParams)?RPAREN | unaryOp unaryExp;
     */
    @Override
    public Value visitUnaryExp(SysyParser.UnaryExpContext ctx) {
        return super.visitUnaryExp(ctx);
    }

    /**
     * unaryOp : ADD | SUB | EXC;
     */
    @Override
    public Value visitUnaryOp(SysyParser.UnaryOpContext ctx) {
        return super.visitUnaryOp(ctx);
    }

    /**
     * funcRParams    : exp ( COMMA exp )*;
     */
    @Override
    public Value visitFuncRParams(SysyParser.FuncRParamsContext ctx) {
        return super.visitFuncRParams(ctx);
    }

    /**
     * mulExp  : unaryExp | mulExp (MUL | DIV | MOD) unaryExp;
     */
    @Override
    public Value visitMulExp(SysyParser.MulExpContext ctx) {
        return super.visitMulExp(ctx);
    }

    /**
     * addExp  : mulExp | addExp (ADD | SUB)  mulExp;
     */
    @Override
    public Value visitAddExp(SysyParser.AddExpContext ctx) {
        return super.visitAddExp(ctx);
    }

    /**
     * relExp  : addExp | relExp (SLT | SGT | SLE | SGE)  addExp;
     */
    @Override
    public Value visitRelExp(SysyParser.RelExpContext ctx) {
        return super.visitRelExp(ctx);
    }

    /**
     * eqExp : relExp | eqExp (EEQ | UEQ)  relExp;
     */
    @Override
    public Value visitEqExp(SysyParser.EqExpContext ctx) {
        return super.visitEqExp(ctx);
    }

    /**
     * lAndExp : eqExp | lAndExp AND eqExp;
     */
    @Override
    public Value visitLAndExp(SysyParser.LAndExpContext ctx) {
        return super.visitLAndExp(ctx);
    }

    /**
     * lOrExp  : lAndExp | lOrExp OR lAndExp;
     */
    @Override
    public Value visitLOrExp(SysyParser.LOrExpContext ctx) {
        return super.visitLOrExp(ctx);
    }

    /**
     * number : iNT_CONST|fLOAT_CONST;
     */
    @Override
    public Value visitNumber(SysyParser.NumberContext ctx) {
        return super.visitNumber(ctx);
    }

    /**
     * iNT_CONST: DEC_INT_CONST | OCT_INT_CONST | HEX_INT_CONST;
     */
    @Override
    public Value visitINT_CONST(SysyParser.INT_CONSTContext ctx) {
        return super.visitINT_CONST(ctx);
    }

    /**
     * fLOAT_CONST: Decimal_floating_constant | Hexadecimal_floating_constant;
     */
    @Override
    public Value visitFLOAT_CONST(SysyParser.FLOAT_CONSTContext ctx) {
        return super.visitFLOAT_CONST(ctx);
    }
}
