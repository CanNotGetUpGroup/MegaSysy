// Generated from D:/JavaProject/MegaSysy\Sysy.g4 by ANTLR 4.9.2
package frontend;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SysyParser}.
 */
public interface SysyListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SysyParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(SysyParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(SysyParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#compUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompUnit(SysyParser.CompUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#compUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompUnit(SysyParser.CompUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterDecl(SysyParser.DeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitDecl(SysyParser.DeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#constDecl}.
	 * @param ctx the parse tree
	 */
	void enterConstDecl(SysyParser.ConstDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#constDecl}.
	 * @param ctx the parse tree
	 */
	void exitConstDecl(SysyParser.ConstDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#bType}.
	 * @param ctx the parse tree
	 */
	void enterBType(SysyParser.BTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#bType}.
	 * @param ctx the parse tree
	 */
	void exitBType(SysyParser.BTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#constDef}.
	 * @param ctx the parse tree
	 */
	void enterConstDef(SysyParser.ConstDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#constDef}.
	 * @param ctx the parse tree
	 */
	void exitConstDef(SysyParser.ConstDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#constInitVal}.
	 * @param ctx the parse tree
	 */
	void enterConstInitVal(SysyParser.ConstInitValContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#constInitVal}.
	 * @param ctx the parse tree
	 */
	void exitConstInitVal(SysyParser.ConstInitValContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#constExp}.
	 * @param ctx the parse tree
	 */
	void enterConstExp(SysyParser.ConstExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#constExp}.
	 * @param ctx the parse tree
	 */
	void exitConstExp(SysyParser.ConstExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#varDecl}.
	 * @param ctx the parse tree
	 */
	void enterVarDecl(SysyParser.VarDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#varDecl}.
	 * @param ctx the parse tree
	 */
	void exitVarDecl(SysyParser.VarDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#varDef}.
	 * @param ctx the parse tree
	 */
	void enterVarDef(SysyParser.VarDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#varDef}.
	 * @param ctx the parse tree
	 */
	void exitVarDef(SysyParser.VarDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#initVal}.
	 * @param ctx the parse tree
	 */
	void enterInitVal(SysyParser.InitValContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#initVal}.
	 * @param ctx the parse tree
	 */
	void exitInitVal(SysyParser.InitValContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#funcDef}.
	 * @param ctx the parse tree
	 */
	void enterFuncDef(SysyParser.FuncDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#funcDef}.
	 * @param ctx the parse tree
	 */
	void exitFuncDef(SysyParser.FuncDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#funcType}.
	 * @param ctx the parse tree
	 */
	void enterFuncType(SysyParser.FuncTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#funcType}.
	 * @param ctx the parse tree
	 */
	void exitFuncType(SysyParser.FuncTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#funcFParams}.
	 * @param ctx the parse tree
	 */
	void enterFuncFParams(SysyParser.FuncFParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#funcFParams}.
	 * @param ctx the parse tree
	 */
	void exitFuncFParams(SysyParser.FuncFParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#funcFParam}.
	 * @param ctx the parse tree
	 */
	void enterFuncFParam(SysyParser.FuncFParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#funcFParam}.
	 * @param ctx the parse tree
	 */
	void exitFuncFParam(SysyParser.FuncFParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(SysyParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(SysyParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#blockItem}.
	 * @param ctx the parse tree
	 */
	void enterBlockItem(SysyParser.BlockItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#blockItem}.
	 * @param ctx the parse tree
	 */
	void exitBlockItem(SysyParser.BlockItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(SysyParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(SysyParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterExp(SysyParser.ExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitExp(SysyParser.ExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#cond}.
	 * @param ctx the parse tree
	 */
	void enterCond(SysyParser.CondContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#cond}.
	 * @param ctx the parse tree
	 */
	void exitCond(SysyParser.CondContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#lVal}.
	 * @param ctx the parse tree
	 */
	void enterLVal(SysyParser.LValContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#lVal}.
	 * @param ctx the parse tree
	 */
	void exitLVal(SysyParser.LValContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#primaryExp}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExp(SysyParser.PrimaryExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#primaryExp}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExp(SysyParser.PrimaryExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(SysyParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(SysyParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#iNT_CONST}.
	 * @param ctx the parse tree
	 */
	void enterINT_CONST(SysyParser.INT_CONSTContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#iNT_CONST}.
	 * @param ctx the parse tree
	 */
	void exitINT_CONST(SysyParser.INT_CONSTContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#fLOAT_CONST}.
	 * @param ctx the parse tree
	 */
	void enterFLOAT_CONST(SysyParser.FLOAT_CONSTContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#fLOAT_CONST}.
	 * @param ctx the parse tree
	 */
	void exitFLOAT_CONST(SysyParser.FLOAT_CONSTContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#unaryExp}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExp(SysyParser.UnaryExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#unaryExp}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExp(SysyParser.UnaryExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#unaryOp}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOp(SysyParser.UnaryOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#unaryOp}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOp(SysyParser.UnaryOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#funcRParams}.
	 * @param ctx the parse tree
	 */
	void enterFuncRParams(SysyParser.FuncRParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#funcRParams}.
	 * @param ctx the parse tree
	 */
	void exitFuncRParams(SysyParser.FuncRParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#mulExp}.
	 * @param ctx the parse tree
	 */
	void enterMulExp(SysyParser.MulExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#mulExp}.
	 * @param ctx the parse tree
	 */
	void exitMulExp(SysyParser.MulExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#addExp}.
	 * @param ctx the parse tree
	 */
	void enterAddExp(SysyParser.AddExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#addExp}.
	 * @param ctx the parse tree
	 */
	void exitAddExp(SysyParser.AddExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#relExp}.
	 * @param ctx the parse tree
	 */
	void enterRelExp(SysyParser.RelExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#relExp}.
	 * @param ctx the parse tree
	 */
	void exitRelExp(SysyParser.RelExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#eqExp}.
	 * @param ctx the parse tree
	 */
	void enterEqExp(SysyParser.EqExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#eqExp}.
	 * @param ctx the parse tree
	 */
	void exitEqExp(SysyParser.EqExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#lAndExp}.
	 * @param ctx the parse tree
	 */
	void enterLAndExp(SysyParser.LAndExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#lAndExp}.
	 * @param ctx the parse tree
	 */
	void exitLAndExp(SysyParser.LAndExpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SysyParser#lOrExp}.
	 * @param ctx the parse tree
	 */
	void enterLOrExp(SysyParser.LOrExpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SysyParser#lOrExp}.
	 * @param ctx the parse tree
	 */
	void exitLOrExp(SysyParser.LOrExpContext ctx);
}