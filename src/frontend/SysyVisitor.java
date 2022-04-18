// Generated from D:/JavaProject/MegaSysy\Sysy.g4 by ANTLR 4.9.2
package frontend;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SysyParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SysyVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SysyParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(SysyParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#compUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompUnit(SysyParser.CompUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecl(SysyParser.DeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#constDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDecl(SysyParser.ConstDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#bType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBType(SysyParser.BTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#constDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDef(SysyParser.ConstDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#constInitVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstInitVal(SysyParser.ConstInitValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#constExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstExp(SysyParser.ConstExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#varDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDecl(SysyParser.VarDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#varDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarDef(SysyParser.VarDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#initVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitVal(SysyParser.InitValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#funcDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncDef(SysyParser.FuncDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#funcType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncType(SysyParser.FuncTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#funcFParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncFParams(SysyParser.FuncFParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#funcFParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncFParam(SysyParser.FuncFParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(SysyParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#blockItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockItem(SysyParser.BlockItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(SysyParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExp(SysyParser.ExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#cond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCond(SysyParser.CondContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#lVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLVal(SysyParser.LValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#primaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExp(SysyParser.PrimaryExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(SysyParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#iNT_CONST}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitINT_CONST(SysyParser.INT_CONSTContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#fLOAT_CONST}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFLOAT_CONST(SysyParser.FLOAT_CONSTContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#unaryExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExp(SysyParser.UnaryExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#unaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(SysyParser.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#funcRParams}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncRParams(SysyParser.FuncRParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#mulExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulExp(SysyParser.MulExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#addExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExp(SysyParser.AddExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#relExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelExp(SysyParser.RelExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#eqExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqExp(SysyParser.EqExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#lAndExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLAndExp(SysyParser.LAndExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SysyParser#lOrExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLOrExp(SysyParser.LOrExpContext ctx);
}