package frontend;

import ir.*;
import ir.DerivedTypes.*;
import ir.Constants.*;
import ir.Instruction.*;
import ir.Module;
import ir.instructions.*;
import util.MyIRBuilder;
import util.SymbolTable;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

public class Visitor extends SysyBaseVisitor<Value> {
    static MyIRBuilder builder;
    MyContext context = MyContext.getInstance();
    SymbolTable symbolTable = SymbolTable.getInstance();
    Module module = Module.getInstance();
    Value curVal;
    Function curF;
    Type type;
    ArrayType arrayType; //声明的数组类型
    ArrayList<Type> paramTypes; //参数类型
    ArrayList<Value> paramList; //参数列表
    boolean enterBlock; //是否进入block，用于函数参数的初始化

    Logger logger = Logger.getLogger(Visitor.class.getName());

    Value LogError(String str) {
        logger.severe(str);
        System.exit(1);
        return null;
    }

    @Override
    public Value visitProgram(SysyParser.ProgramContext ctx) {
        ArrayList<Type> param_array = new ArrayList<>();
        param_array.add(PointerType.get(Type.getInt32Ty()));
        ArrayList<Type> param_int = new ArrayList<>();
        param_int.add(Type.getInt32Ty());
        ArrayList<Type> param_put_array = new ArrayList<>();
        param_put_array.add(Type.getInt32Ty());
        param_put_array.add(PointerType.get(Type.getInt32Ty()));
        ArrayList<Type> param_memset = new ArrayList<>();
        param_memset.add(PointerType.get(Type.getInt32Ty()));
        param_memset.add(Type.getInt32Ty());
        param_memset.add(Type.getInt32Ty());

        symbolTable.addValue("getint", builder.createFunction(FunctionType.get(Type.getInt32Ty()), "getint", module));
        symbolTable.addValue("getarray", builder.createFunction(FunctionType.get(Type.getInt32Ty(), param_array), "getarray", module));
        symbolTable.addValue("getch", builder.createFunction(FunctionType.get(Type.getInt32Ty()), "getch", module));
        symbolTable.addValue("putint", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_int), "putint", module));
        symbolTable.addValue("putch", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_int), "putch", module));
        symbolTable.addValue("putarray", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_put_array), "putarray", module));
        symbolTable.addValue("memset", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_memset), "memset", module));

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
        return super.visitConstDecl(ctx);
    }

    /**
     * bType : INT | FLOAT;
     */
    @Override
    public Value visitBType(SysyParser.BTypeContext ctx) {
        String bType = ctx.getText();
        if (bType.equals("int")) {
            type = Type.getInt32Ty();
        } else {
            type = Type.getFloatTy();
        }
        return super.visitBType(ctx);
    }

    /**
     * constDef : IDENT (LBRACKET constExp RBRACKET )* EQ constInitVal;
     */
    @Override
    public Value visitConstDef(SysyParser.ConstDefContext ctx) {
        String name = ctx.IDENT().getText();
        if (symbolTable.getCurTable().get(name) != null) {
            LogError(name + "在此作用域已存在");
        }
        if (ctx.constExp().size() != 0) {//数组
            visit(ctx.constExp(ctx.constExp().size() - 1));
            if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).getVal() >= 0)) {
                LogError("数组各维长度应为非负整数");
            }
            arrayType = ArrayType.get(type, ((ConstantInt) curVal).getVal());
            for (int i = ctx.constExp().size() - 2; i >= 0; i--) {
                visit(ctx.constExp(i));
                if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).getVal() >= 0)) {
                    LogError("数组各维长度应为非负整数");
                }
                arrayType = ArrayType.get(arrayType, ((ConstantInt) curVal).getVal());
            }
            ctx.constInitVal().arrayType = arrayType;
            Type ty=arrayType;
            typeLinkedList=new ArrayList<>();
            while(ty.isArrayTy()){
                typeLinkedList.add(ty);
                ty=((ArrayType)ty).getKidType();
            }
            typeLinkedList.add(ty);
            depth=0;
            visit(ctx.constInitVal());

            if (symbolTable.inGlobalArea()) {
                curVal = builder.createGlobalVariable(arrayType, module, (Constant) curVal, true);
                symbolTable.addValue(name, curVal);
            } else {
                Value val = builder.createAlloca(arrayType);
                //TODO

            }
        } else {
            visit(ctx.constInitVal());
            if (!curVal.getType().equals(type)) {
                LogError("赋值类型与定义不匹配");
            }
            symbolTable.addValue(name, curVal);
        }
        return curVal;
    }

    ArrayList<Type> typeLinkedList;
    int depth;

    /**
     * constInitVal : constExp | LBRACE(constInitVal (COMMA constInitVal)*)?RBRACE;
     */
    @Override
    public Value visitConstInitVal(SysyParser.ConstInitValContext ctx) {
        if (ctx.children.size() == 1) {//constExp
            visit(ctx.constExp());
        } else {
            if (!ctx.arrayType.isArrayTy()) {
                LogError("初始化类型与定义不符");
            }
            depth++;
            ArrayType arrTy = (ArrayType) ctx.arrayType;
            Type eleType = typeLinkedList.get(typeLinkedList.size()-1-depth);
            Value tmpArr = null;
            Type tmpArrTy = null;
            ArrayList<Value> tmpArrList = null;
            int arr_site = 0;
            Type UpArrTy = null;
            ArrayList<Value> UpArrList = new ArrayList<>();

            int numEle = arrTy.getNumElements();
            int eleSize = arrTy.getEleSize();
            int pos = 0;
            ArrayList<Value> V = new ArrayList<>();
            for (int i = 0; i < ctx.constInitVal().size(); i++) {
                ctx.constInitVal(i).arrayType = typeLinkedList.get(typeLinkedList.size()-1-depth);
                if(ctx.constInitVal(i).children.size()==1){
                    visit(ctx.constInitVal(i));
                    if (!type.equals(curVal.getType()) && !(type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty()))) {
                        LogError("赋值类型与定义不符");
                    }
                    //整数转浮点数
                    if (type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty())) {
                        curVal = builder.createCast(Ops.SIToFP, curVal, Type.getFloatTy());
                    }

                    if (ctx.constInitVal(i).arrayType.isArrayTy()) {
                        if (tmpArrList == null) {
                            tmpArrList = new ArrayList<>();
                            tmpArrTy = ctx.constInitVal(i).arrayType;
                            while(((ArrayType)tmpArrTy).getKidType().isArrayTy()){
                                tmpArrTy=((ArrayType) tmpArrTy).getKidType();
                            }
                        } else if (!curVal.getType().equals(tmpArrTy)) {
                            LogError("数组初始化未对齐");
                        }
                        tmpArrList.add(curVal);
                        arr_site++;
                        if (arr_site % ((ArrayType)tmpArrTy).getNumElements() == 0) {

                            tmpArr = ConstantArray.get((ArrayType)tmpArrTy, tmpArrList);
                            tmpArrList = null;
                            UpArrList.add(tmpArr);
                        }
                    } else {
                        V.add(curVal);
                        UpArrList.add(curVal);
                    }
                }else{
                    visit(ctx.constInitVal(i));

                }

            }
            curVal = ConstantArray.get(curVal.getType(), V);
            depth--;
        }
        return curVal;
    }

    public boolean checkConstArrayInit(ArrayType ATy, ConstantArray constantArray) {
        ArrayList<Value> valList = constantArray.getOperandList();
        Type eleTy = ATy.getKidType();

        for (Value v : valList) {

        }
        return true;
    }

    /**
     * constExp : addExp ;
     */
    @Override
    public Value visitConstExp(SysyParser.ConstExpContext ctx) {
        visitAddExp(ctx.addExp());
        if (!(curVal instanceof Constant)) {
            LogError(("constExp的赋值必须为常量！"));
        }
        return curVal;
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
        String name = ctx.IDENT().getText();
        if (symbolTable.getCurTable().get(name) != null) {
            LogError(name + "在此作用域已存在");
        }
        if (ctx.constExp().size() != 0) {//数组
            visit(ctx.constExp(ctx.constExp().size() - 1));
            if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).getVal() >= 0)) {
                LogError("数组各维长度应为非负整数");
            }
            arrayType = ArrayType.get(type, ((ConstantInt) curVal).getVal());
            for (int i = ctx.constExp().size() - 2; i >= 0; i--) {
                visit(ctx.constExp(i));
                if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).getVal() >= 0)) {
                    LogError("数组各维长度应为非负整数");
                }
                arrayType = ArrayType.get(arrayType, ((ConstantInt) curVal).getVal());
            }
            if (ctx.initVal() != null) {
                visit(ctx.initVal());
                if (symbolTable.inGlobalArea()) {
                    curVal = builder.createGlobalVariable(arrayType, module, (Constant) curVal, false);
                    symbolTable.addValue(name, curVal);
                } else {
                    Value alloca = builder.createAlloca(arrayType);

                    //TODO

                }
            } else {

            }
        } else {
            if (ctx.initVal() != null) {
                if (!curVal.getType().equals(type)) {
                    LogError("赋值类型与定义不匹配");
                }
                if (symbolTable.inGlobalArea()) {
                    visit(ctx.initVal());
                    Value val = builder.createGlobalVariable(type, module, (Constant) curVal, false);
                    symbolTable.addValue(name, val);
                } else {
                    Value alloca = builder.createAlloca(type);
                    symbolTable.addValue(name, alloca);
                    visit(ctx.initVal());
                    builder.createStore(curVal, alloca);
                    ((Instructions.AllocaInst) alloca).setUndef(true);
                }
            } else {
                if (symbolTable.inGlobalArea()) {
                    Value val = builder.createGlobalVariable(type, module, Constant.getNullValue(type), false);
                    symbolTable.addValue(name, val);
                } else {
                    Value alloca = builder.createAlloca(type);
                    symbolTable.addValue(name, alloca);
                }
            }
        }

        return curVal;
    }

    /**
     * initVal : exp | LBRACE (initVal (COMMA initVal)*)? RBRACE;
     */
    @Override
    public Value visitInitVal(SysyParser.InitValContext ctx) {
        if (ctx.children.size() == 1) {//exp
            visit(ctx.exp());
        } else {
            ArrayList<Value> V = new ArrayList<>();
            for (int i = 0; i < ctx.initVal().size(); i++) {
                visit(ctx.initVal(i));
                if (!type.equals(curVal.getType()) && !(type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty()))) {
                    LogError("赋值类型与定义不符");
                }
                //TODO:整数转浮点数
                if (type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty())) {
                    curVal = builder.createCast(Instruction.Ops.IntToPtr, curVal, Type.getFloatTy());
                }
                V.add(curVal);
            }
            curVal = ConstantArray.get(curVal.getType(), V);
        }
        return curVal;
    }

    /**
     * funcDef : funcType IDENT LPAREN (funcFParams)? RPAREN  block;
     */
    @Override
    public Value visitFuncDef(SysyParser.FuncDefContext ctx) {
        visit(ctx.funcType());
        Type retType = type;
        enterBlock = false;
        String name = ctx.IDENT().getText();
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
        } else {
            paramTypes = null;
        }
        FunctionType FT = FunctionType.get(retType, paramTypes);
        curF = builder.createFunction(FT, name, module);
        symbolTable.addValue(name, curF);
        symbolTable.addLevel();
        BasicBlock BB = builder.createBasicBlock("entry", curF);
        enterBlock = true;
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
        }
        enterBlock = false;
        visit(ctx.block());
        return null;
    }

    /**
     * funcType : INT|VOID|FLOAT;
     */
    @Override
    public Value visitFuncType(SysyParser.FuncTypeContext ctx) {
        String funcType = ctx.getText();
        if (funcType.equals("int")) {
            type = Type.getInt32Ty();
        } else if (funcType.equals("float")) {
            type = Type.getFloatTy();
        } else {
            type = Type.getVoidTy();
        }
        return null;
    }

    /**
     * funcFParams : funcFParam ( COMMA funcFParam )*;
     */
    @Override
    public Value visitFuncFParams(SysyParser.FuncFParamsContext ctx) {
        ArrayList<Type> tmpParamTypes = new ArrayList<>();
        for (int i = 0; i < ctx.funcFParam().size(); i++) {
            visit(ctx.funcFParam(i));
            tmpParamTypes.add(type);
            if (enterBlock) {
                ((Argument) curVal).setArgNo(i);
                curF.getArguments().add((Argument) curVal);
            }
        }
        paramTypes = tmpParamTypes;
        return null;
    }

    /**
     * funcFParam : bType IDENT  (LBRACKET RBRACKET ( LBRACKET exp RBRACKET )*)?;
     */
    @Override
    public Value visitFuncFParam(SysyParser.FuncFParamContext ctx) {
        visit(ctx.bType());
        if (!ctx.LBRACKET().isEmpty()) {
            for (int i = 0; i < ctx.exp().size(); i++) {
                visit(ctx.exp(i));
                type = ArrayType.get(type, ((ConstantInt) curVal).getVal());
            }
            type = PointerType.get(type);
        }
        if (enterBlock) {
            String name = ctx.IDENT().getText();
            curVal = new Argument(type, curF, 0);
            Value alloc = builder.createAlloca(type);
            builder.createStore(curVal, alloc);
            symbolTable.addValue(name, alloc);
        }
        return curVal;
    }

    /**
     * block : LBRACE ( blockItem )* RBRACE;
     */
    @Override
    public Value visitBlock(SysyParser.BlockContext ctx) {
        symbolTable.addLevel();
        super.visitBlock(ctx);
        symbolTable.removeTop();
        return null;
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
     * | block
     * | (exp)? SEMICOLON
     * | IF LPAREN cond RPAREN stmt ( ELSE stmt)?
     * | WHILE LPAREN  cond RPAREN  stmt
     * | BREAK SEMICOLON
     * | CONTINUE SEMICOLON
     * | RETURN (exp)? SEMICOLON ;
     */
    @Override
    public Value visitStmt(SysyParser.StmtContext ctx) {
        switch (ctx.children.size()) {
            case 4://LVal '=' Exp ';'
                Value lhs = visit(ctx.lVal());
                Value rhs = visit(ctx.exp());
                builder.createStore(rhs, lhs);
                break;
            case 1:
                if (ctx.block() != null) {
                    visit(ctx.block());
                }
                break;
            case 2:
                if (ctx.exp() != null) {//Exp;
                    visit(ctx.exp());
                } else if (ctx.BREAK() != null) {//break;

                } else if (ctx.CONTINUE() != null) {//continue;

                } else {//return ';'
                    builder.createRetVoid();
                }
                break;
            case 5:
            case 7:
                if (ctx.IF() != null) {//'if' '(' Cond ')' Stmt [ 'else' Stmt ]

                } else {//WHILE LPAREN  cond RPAREN  stmt

                }
                break;
            case 3://'return' Exp ';'
                visit(ctx.exp());
                builder.createRet(curVal);
                break;
        }
        return null;
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
        String name = ctx.IDENT().getText();
        curVal = symbolTable.lookup(name);
        if (curVal == null) {
            return LogError("未找到名为: " + name + " 的变量");
        }

        return curVal;
    }

    /**
     * primaryExp : LPAREN exp RPAREN | lVal | number ;
     */
    @Override
    public Value visitPrimaryExp(SysyParser.PrimaryExpContext ctx) {
        if (ctx.exp() != null) {
            visit(ctx.exp());
        } else if (ctx.lVal() != null) {
            visit(ctx.lVal());
        } else {
            visit(ctx.number());
        }
        return curVal;
    }

    /**
     * unaryExp  : primaryExp | IDENT LPAREN(funcRParams)?RPAREN | unaryOp unaryExp;
     */
    @Override
    public Value visitUnaryExp(SysyParser.UnaryExpContext ctx) {
        if (ctx.primaryExp() != null) {
            visit(ctx.primaryExp());
        } else if (ctx.unaryOp() != null) {
            visit(ctx.unaryExp());
            Value tmp = curVal;
//            if(tmp.getType().equals(Type.getInt1Ty())){
//                curVal=builder.createZExt(tmp,Type.getInt32Ty());
//            }
            if (ctx.unaryOp().SUB() != null) {
                if (tmp.getType().isInt1Ty()) {
                    tmp = builder.createZExt(tmp, Type.getInt32Ty());
                }
                if (tmp.getType().isInt32Ty()) {
                    curVal = builder.createSub(ConstantInt.const_0(), tmp);
                } else if (tmp.getType().isFloatTy()) {
                    curVal = builder.createFSub(ConstantFP.const_0(), tmp);
                }
            } else if (ctx.unaryOp().EXC() != null) {
                if (tmp.getType().isInt32Ty()) {
                    curVal = builder.createICmpEQ(tmp, ConstantInt.const_0());
                } else if (tmp.getType().isFloatTy()) {
                    curVal = builder.createFCmpUEQ(tmp, ConstantFP.const_0());
                } else if (tmp.getType().isInt1Ty()) {
                    curVal = builder.createNot(tmp);
                }
            }
        } else {//Ident '('[FuncRParams]')'
            String name = ctx.IDENT().getText();
            Function F = symbolTable.getFunction(name);
            paramList = new ArrayList<>();

            if (F == null) {
                LogError("未找到名为" + symbolTable.getName(F) + "的函数");
            }
            FunctionType FT = (FunctionType) F.getType();
            if (FT.getContainedTys().size() - 1 != ctx.children.size() - 2) {//参数数量不对
                LogError("函数" + symbolTable.getName(F) + "参数数量不对");
            }
            if (ctx.funcRParams() != null) {//F(a,...)
                type = FT;
                visit(ctx.funcRParams());
            }
            curVal = builder.createCall(F, paramList);
        }
        return curVal;
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
        FunctionType FT = (FunctionType) type;
        for (int i = 0; i < ctx.exp().size(); i++) {
            visit(ctx.exp(i));
            if (!curVal.getType().equals(FT.getContainedTys().get(i + 1))) {
                LogError("函数引用时，参数类型错误");
            }
            paramList.add(curVal);
        }
        return null;
    }

    /**
     * mulExp  : unaryExp | mulExp (MUL | DIV | MOD) unaryExp;
     */
    @Override
    public Value visitMulExp(SysyParser.MulExpContext ctx) {
        if (ctx.children.size() == 1) {
            visit(ctx.unaryExp());
        } else {
            Value L, R;
            L = visit(ctx.mulExp());
            R = visit(ctx.unaryExp());
            if (ctx.MUL() != null) {
                curVal = builder.createBinary(Ops.Mul, L, R);
            } else if (ctx.DIV() != null) {
                curVal = builder.createBinary(Ops.SDiv, L, R);
            } else if (ctx.MOD() != null) {
                curVal = builder.createBinary(Ops.SRem, L, R);
            }
        }
        return curVal;
    }

    /**
     * addExp  : mulExp | addExp (ADD | SUB)  mulExp;
     */
    @Override
    public Value visitAddExp(SysyParser.AddExpContext ctx) {
        if (ctx.children.size() == 1) {
            visit(ctx.mulExp());
        } else {
            Value L, R;
            L = visit(ctx.addExp());
            R = visit(ctx.mulExp());
            if (ctx.ADD() != null) {
                curVal = builder.createBinary(Ops.Add, L, R);
            } else if (ctx.SUB() != null) {
                curVal = builder.createBinary(Ops.Sub, L, R);
            }
        }
        return curVal;
    }

    /**
     * relExp  : addExp | relExp (SLT | SGT | SLE | SGE)  addExp;
     */
    @Override
    public Value visitRelExp(SysyParser.RelExpContext ctx) {
        if (ctx.children.size() == 1) {
            visit(ctx.addExp());
        } else {
            Value L, R;
            L = visit(ctx.relExp());
            R = visit(ctx.addExp());
            if (ctx.SLT() != null) {
                curVal = builder.createCmp(CmpInst.Predicate.ICMP_SLT, L, R);
            } else if (ctx.SGT() != null) {
                curVal = builder.createCmp(CmpInst.Predicate.ICMP_SGT, L, R);
            } else if (ctx.SLE() != null) {
                curVal = builder.createCmp(CmpInst.Predicate.ICMP_SLE, L, R);
            } else if (ctx.SGE() != null) {
                curVal = builder.createCmp(CmpInst.Predicate.ICMP_SGE, L, R);
            }
        }
        return curVal;
    }

    /**
     * eqExp : relExp | eqExp (EEQ | UEQ)  relExp;
     */
    @Override
    public Value visitEqExp(SysyParser.EqExpContext ctx) {
        if (ctx.children.size() == 1) {
            visit(ctx.relExp());
        } else {
            Value L, R;
            L = visit(ctx.eqExp());
            R = visit(ctx.relExp());
            if (ctx.EEQ() != null) {
                curVal = builder.createCmp(CmpInst.Predicate.ICMP_EQ, L, R);
            } else if (ctx.UEQ() != null) {
                curVal = builder.createCmp(CmpInst.Predicate.ICMP_NE, L, R);
            }
        }
        return curVal;
    }

    /**
     * lAndExp : eqExp | lAndExp AND eqExp;
     */
    @Override
    public Value visitLAndExp(SysyParser.LAndExpContext ctx) {
        if (ctx.children.size() == 1) {
            visit(ctx.eqExp());
        } else {
            Value L, R;
            L = visit(ctx.lAndExp());
            R = visit(ctx.eqExp());
            if (ctx.AND() != null) {
                curVal = builder.createLogicalAnd(L, R);
            }
        }
        return curVal;
    }

    /**
     * lOrExp  : lAndExp | lOrExp OR lAndExp;
     */
    @Override
    public Value visitLOrExp(SysyParser.LOrExpContext ctx) {
        if (ctx.children.size() == 1) {
            visit(ctx.lAndExp());
        } else {
            Value L, R;
            L = visit(ctx.lOrExp());
            R = visit(ctx.lAndExp());
            if (ctx.OR() != null) {
                curVal = builder.createLogicalOr(L, R);
            }
        }
        return curVal;
    }

    /**
     * number : iNT_CONST|fLOAT_CONST;
     */
    @Override
    public Value visitNumber(SysyParser.NumberContext ctx) {
        if (ctx.iNT_CONST() != null) {
            String text = ctx.iNT_CONST().getText();
            type = Type.getInt32Ty();
            int number = new BigInteger(text).intValue();
            curVal = ConstantInt.get(number);
        } else {
            String text = ctx.fLOAT_CONST().getText();
            type = Type.getFloatTy();
            float number = new BigDecimal(text).floatValue();
            curVal = ConstantFP.get(number);
        }
        return curVal;
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
