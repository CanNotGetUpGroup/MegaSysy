package frontend;

import ir.Module;
import ir.MyContext;
import ir.Value;
import util.MyIRBuilder;

import java.util.HashMap;
import java.util.logging.Logger;

public class Visitor extends SysyBaseVisitor<Value> {
    static MyIRBuilder builder;
    static MyContext context = MyContext.getInstance();
    static Module module;
    static HashMap<String, Value> NamedValues;
    static Logger logger;
    static Value LogError(String str){
        System.out.println(str);
        return null;
    }

    @Override
    public Value visitProgram(SysyParser.ProgramContext ctx) {
        return super.visitProgram(ctx);
    }

    @Override
    public Value visitCompUnit(SysyParser.CompUnitContext ctx) {
        return super.visitCompUnit(ctx);
    }

    @Override
    public Value visitDecl(SysyParser.DeclContext ctx) {
        return super.visitDecl(ctx);
    }

    @Override
    public Value visitConstDecl(SysyParser.ConstDeclContext ctx) {
        return super.visitConstDecl(ctx);
    }

    @Override
    public Value visitBType(SysyParser.BTypeContext ctx) {
        return super.visitBType(ctx);
    }

    @Override
    public Value visitConstDef(SysyParser.ConstDefContext ctx) {
        return super.visitConstDef(ctx);
    }

    @Override
    public Value visitConstInitVal(SysyParser.ConstInitValContext ctx) {
        return super.visitConstInitVal(ctx);
    }

    @Override
    public Value visitConstExp(SysyParser.ConstExpContext ctx) {
        return super.visitConstExp(ctx);
    }

    @Override
    public Value visitVarDecl(SysyParser.VarDeclContext ctx) {
        return super.visitVarDecl(ctx);
    }

    @Override
    public Value visitVarDef(SysyParser.VarDefContext ctx) {
        return super.visitVarDef(ctx);
    }

    @Override
    public Value visitInitVal(SysyParser.InitValContext ctx) {
        return super.visitInitVal(ctx);
    }

    @Override
    public Value visitFuncDef(SysyParser.FuncDefContext ctx) {
        return super.visitFuncDef(ctx);
    }

    @Override
    public Value visitFuncType(SysyParser.FuncTypeContext ctx) {
        return super.visitFuncType(ctx);
    }

    @Override
    public Value visitFuncFParams(SysyParser.FuncFParamsContext ctx) {
        return super.visitFuncFParams(ctx);
    }

    @Override
    public Value visitFuncFParam(SysyParser.FuncFParamContext ctx) {
        return super.visitFuncFParam(ctx);
    }

    @Override
    public Value visitBlock(SysyParser.BlockContext ctx) {
        return super.visitBlock(ctx);
    }

    @Override
    public Value visitBlockItem(SysyParser.BlockItemContext ctx) {
        return super.visitBlockItem(ctx);
    }

    @Override
    public Value visitStmt(SysyParser.StmtContext ctx) {
        return super.visitStmt(ctx);
    }

    @Override
    public Value visitExp(SysyParser.ExpContext ctx) {
        return super.visitExp(ctx);
    }

    @Override
    public Value visitCond(SysyParser.CondContext ctx) {
        return super.visitCond(ctx);
    }

    @Override
    public Value visitLVal(SysyParser.LValContext ctx) {
        return super.visitLVal(ctx);
    }

    @Override
    public Value visitPrimaryExp(SysyParser.PrimaryExpContext ctx) {
        return super.visitPrimaryExp(ctx);
    }

    @Override
    public Value visitUnaryExp(SysyParser.UnaryExpContext ctx) {
        return super.visitUnaryExp(ctx);
    }

    @Override
    public Value visitUnaryOp(SysyParser.UnaryOpContext ctx) {
        return super.visitUnaryOp(ctx);
    }

    @Override
    public Value visitFuncRParams(SysyParser.FuncRParamsContext ctx) {
        return super.visitFuncRParams(ctx);
    }

    @Override
    public Value visitMulExp(SysyParser.MulExpContext ctx) {
        return super.visitMulExp(ctx);
    }

    @Override
    public Value visitAddExp(SysyParser.AddExpContext ctx) {
        return super.visitAddExp(ctx);
    }

    @Override
    public Value visitRelExp(SysyParser.RelExpContext ctx) {
        return super.visitRelExp(ctx);
    }

    @Override
    public Value visitEqExp(SysyParser.EqExpContext ctx) {
        return super.visitEqExp(ctx);
    }

    @Override
    public Value visitLAndExp(SysyParser.LAndExpContext ctx) {
        return super.visitLAndExp(ctx);
    }

    @Override
    public Value visitLOrExp(SysyParser.LOrExpContext ctx) {
        return super.visitLOrExp(ctx);
    }
}
