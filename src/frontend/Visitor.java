package frontend;

import ir.*;
import ir.DerivedTypes.*;
import ir.Constants.*;
import ir.Instruction.*;
import ir.Module;
import ir.instructions.*;
import util.MyIRBuilder;
import util.SymbolTable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

public class Visitor extends SysyBaseVisitor<Value> {
    static MyIRBuilder builder = MyIRBuilder.getInstance();
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
        throw new RuntimeException("str");
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

        symbolTable.addValue("getint", builder.createFunction(FunctionType.get(Type.getInt32Ty()), "getint", module, false));
        symbolTable.addValue("getarray", builder.createFunction(FunctionType.get(Type.getInt32Ty(), param_array), "getarray", module, false));
        symbolTable.addValue("getch", builder.createFunction(FunctionType.get(Type.getInt32Ty()), "getch", module, false));
        symbolTable.addValue("putint", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_int), "putint", module, false));
        symbolTable.addValue("putch", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_int), "putch", module, false));
        symbolTable.addValue("putarray", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_put_array), "putarray", module, false));
        symbolTable.addValue("memset", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_memset), "memset", module, false));

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
            ArrayType Aty;
            if (!symbolTable.inGlobalArea()) {
                Value alloca = builder.createAlloca(arrayType);
                symbolTable.addValue(name, alloca);
                Aty = (ArrayType) arrayType.getKidType();
                ((Instructions.AllocaInst) alloca).setUndef(false);
                //利用memset初始化数组为0
                gep = builder.createGEP(alloca, new ArrayList<>() {{
                    add(ConstantInt.const_0());
                    add(ConstantInt.const_0());
                }});
                while (Aty.getKidType().isArrayTy()) {
                    gep = builder.createGEP(gep, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(ConstantInt.const_0());
                    }});
                    Aty = (ArrayType) Aty.getKidType();
                }
                ArrayList<Value> Args = new ArrayList<>();
                Args.add(gep);
                Args.add(Constant.getNullValue(type));
                Args.add(ConstantInt.get(arrayType.getNumElements() * arrayType.getEleSize() * 4));
                builder.createCall(symbolTable.getFunction("memset"), Args);
            }

            typeArrayList = new ArrayList<>();
            Aty = arrayType;
            while (Aty.getKidType().isArrayTy()) {
                typeArrayList.add(Aty);
                Aty = (ArrayType) Aty.getKidType();
            }
            tmpArrList = new ArrayList<>();
            getDimInfo = false;
            visit(ctx.constInitVal());
            getDimInfo = true;
            visit(ctx.constInitVal());
            //int a[2][2]={1,2,3}，生成的tmpArrList中只有{1,2,3}，补齐0
            while (tmpArrList.size() < arrayType.getNumElements() * arrayType.getEleSize()) {
                tmpArrList.add(Constant.getNullValue(type));
            }
            curVal = changeArrType(arrayType, tmpArrList);

            if (symbolTable.inGlobalArea()) {
                curVal = builder.createGlobalVariable("@" + name, arrayType, module, (Constant) curVal, true);
                symbolTable.addValue(name, curVal);
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

    ArrayList<ArrayType> typeArrayList;
    Value gep;
    ArrayList<Value> tmpArrList = null;

    boolean getDimInfo;

    /**
     * constInitVal : constExp | LBRACE(constInitVal (COMMA constInitVal)*)?RBRACE;
     */
    @Override
    public Value visitConstInitVal(SysyParser.ConstInitValContext ctx) {
        if (ctx.children.size() == 1) {//constExp
            visit(ctx.constExp());
        } else {
            if (!getDimInfo) {
                int kidDim = 0;
                for (int i = 0; i < ctx.constInitVal().size(); i++) {
                    if (ctx.constInitVal(i).children.size() != 1) {
                        visit(ctx.constInitVal(i));
                        kidDim = Math.max(kidDim, ((ArrayType) ctx.constInitVal(i).arrayType).getDim());
                    }
                }
                assert typeArrayList.size() - 1 - kidDim > 0;
                ctx.arrayType = typeArrayList.get(typeArrayList.size() - 1 - kidDim);
            } else {
                ArrayType arrTy = (ArrayType) ctx.arrayType;
                int numEle = arrTy.getNumElements();
                int eleSize = arrTy.getEleSize();
                ArrayList<Value> V = new ArrayList<>();
                for (int i = 0; i < ctx.constInitVal().size(); i++) {
                    if (ctx.constInitVal(i).children.size() == 1) {
                        visit(ctx.constInitVal(i));
                        if (!type.equals(curVal.getType()) && !(type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty()))) {
                            LogError("赋值类型与定义不符");
                        }
                        //整数转浮点数
                        if (type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty())) {
                            curVal = builder.createCast(Ops.SIToFP, curVal, Type.getFloatTy());
                        }
                        if (!symbolTable.inGlobalArea()) {
                            if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).isZero())) {
                                if (V.isEmpty()) {
                                    builder.createStore(curVal, gep);
                                } else {
                                    Value ptr = builder.createGEP(gep, new ArrayList<>() {{
                                        add(ConstantInt.get(V.size()));
                                    }});
                                    builder.createStore(curVal, ptr);
                                }
                            }
                        }
                        V.add(curVal);
                    } else {
                        //a[2][2][2]={1,{2,3}}={{{1,0},{2,3}},{{0,0},{0,0}}}
                        //a[2][2][2]={1,{{2,3}}}={{{1,0},{0,0}},{{2,3},{0,0}}}
                        for (int j = V.size() % eleSize; j > 0; j = (j + 1) % eleSize) {
                            V.add(Constant.getNullValue(type));
                        }
                        visit(ctx.constInitVal(i));
                        V.addAll(tmpArrList);
                    }
                }
                while (V.size() < numEle * eleSize) {
                    V.add(Constant.getNullValue(type));
                }
                tmpArrList = V;
            }
        }
        return curVal;
    }

    public ConstantArray changeArrType(ArrayType target, ArrayList<Value> V) {
        ArrayList<Value> ret = new ArrayList<>();
        int eleSize = target.getEleSize();
        int numEle = target.getNumElements();
        if (target.getKidType().isArrayTy()) {
            for (int i = 0; i < numEle; i++) {
                ret.add(changeArrType((ArrayType) target.getKidType(), new ArrayList<>(V.subList(i * eleSize, (i + 1) * eleSize))));
            }
        } else {
            for (int i = 0; i < numEle; i++) {
                ret.add(V.get(i));
            }
        }

        return ConstantArray.get(target, ret);
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
                ArrayType Aty;
                if (!symbolTable.inGlobalArea()) {
                    Value alloca = builder.createAlloca(arrayType);
                    symbolTable.addValue(name, alloca);
                    Aty = (ArrayType) arrayType.getKidType();
                    ((Instructions.AllocaInst) alloca).setUndef(false);
                    //利用memset初始化数组为0
                    gep = builder.createGEP(alloca, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(ConstantInt.const_0());
                    }});
                    while (Aty.getKidType().isArrayTy()) {
                        gep = builder.createGEP(gep, new ArrayList<>() {{
                            add(ConstantInt.const_0());
                            add(ConstantInt.const_0());
                        }});
                        Aty = (ArrayType) Aty.getKidType();
                    }
                    ArrayList<Value> Args = new ArrayList<>();
                    Args.add(gep);
                    Args.add(Constant.getNullValue(type));
                    Args.add(ConstantInt.get(arrayType.getNumElements() * arrayType.getEleSize() * 4));
                    builder.createCall(symbolTable.getFunction("memset"), Args);
                }

                typeArrayList = new ArrayList<>();
                Aty = arrayType;
                while (Aty.getKidType().isArrayTy()) {
                    typeArrayList.add(Aty);
                    Aty = (ArrayType) Aty.getKidType();
                }
                tmpArrList = new ArrayList<>();
                getDimInfo = false;
                visit(ctx.initVal());
                getDimInfo = true;
                visit(ctx.initVal());
                //int a[2][2]={1,2,3}，生成的tmpArrList中只有{1,2,3}，补齐0
                while (tmpArrList.size() < arrayType.getNumElements() * arrayType.getEleSize()) {
                    tmpArrList.add(Constant.getNullValue(type));
                }
                if (symbolTable.inGlobalArea()) {
                    curVal = changeArrType(arrayType, tmpArrList);//全局数组初始化一定是常量
                    curVal = builder.createGlobalVariable("@" + name, arrayType, module, (Constant) curVal, false);
                    symbolTable.addValue(name, curVal);
                }
            } else {
                if (symbolTable.inGlobalArea()) {
                    curVal = builder.createGlobalVariable("@" + name, arrayType, module, Constant.getNullValue(arrayType), false);
                    symbolTable.addValue(name, curVal);
                } else {
                    Value alloca = builder.createAlloca(arrayType);
                    symbolTable.addValue(name, alloca);
                }
            }
        } else {
            if (ctx.initVal() != null) {
                if (symbolTable.inGlobalArea()) {
                    visit(ctx.initVal());
                    if (!curVal.getType().equals(type)) {
                        LogError("赋值类型与定义不匹配");
                    }
                    Value val = builder.createGlobalVariable("@" + name, type, module, (Constant) curVal, false);
                    symbolTable.addValue(name, val);
                } else {
                    Value alloca = builder.createAlloca(type);
                    symbolTable.addValue(name, alloca);
                    visit(ctx.initVal());
                    if (!curVal.getType().equals(type)) {
                        LogError("赋值类型与定义不匹配");
                    }
                    builder.createStore(curVal, alloca);
                    ((Instructions.AllocaInst) alloca).setUndef(false);
                }
            } else {
                if (symbolTable.inGlobalArea()) {
                    Value val = builder.createGlobalVariable("@" + name, type, module, Constant.getNullValue(type), false);
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
            if (!getDimInfo) {
                int kidDim = 0;
                for (int i = 0; i < ctx.initVal().size(); i++) {
                    if (ctx.initVal(i).children.size() != 1) {
                        visit(ctx.initVal(i));
                        kidDim = Math.max(kidDim, ((ArrayType) ctx.initVal(i).arrayType).getDim());
                    }
                }
                assert typeArrayList.size() - 1 - kidDim > 0;
                ctx.arrayType = typeArrayList.get(typeArrayList.size() - 1 - kidDim);
            } else {
                ArrayType arrTy = (ArrayType) ctx.arrayType;
                int numEle = arrTy.getNumElements();
                int eleSize = arrTy.getEleSize();
                ArrayList<Value> V = new ArrayList<>();
                for (int i = 0; i < ctx.initVal().size(); i++) {
                    if (ctx.initVal(i).children.size() == 1) {
                        visit(ctx.initVal(i));
                        if (!type.equals(curVal.getType()) && !(type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty()))) {
                            LogError("赋值类型与定义不符");
                        }
                        //整数转浮点数
                        if (type.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty())) {
                            curVal = builder.createCast(Ops.SIToFP, curVal, Type.getFloatTy());
                        }
                        if (!symbolTable.inGlobalArea()) {
                            if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).isZero())) {
                                if (V.isEmpty()) {
                                    builder.createStore(curVal, gep);
                                } else {
                                    Value ptr = builder.createGEP(gep, new ArrayList<>() {{
                                        add(ConstantInt.get(V.size()));
                                    }});
                                    builder.createStore(curVal, ptr);
                                }
                            }
                        }
                        V.add(curVal);
                    } else {
                        //a[2][2][2]={1,{2,3}}={{{1,0},{2,3}},{{0,0},{0,0}}}
                        //a[2][2][2]={1,{{2,3}}}={{{1,0},{0,0}},{{2,3},{0,0}}}
                        for (int j = V.size() % eleSize; j > 0; j = (j + 1) % eleSize) {
                            V.add(Constant.getNullValue(type));
                        }
                        visit(ctx.initVal(i));
                        V.addAll(tmpArrList);
                    }
                }
                while (V.size() < numEle * eleSize) {
                    V.add(Constant.getNullValue(type));
                }
                tmpArrList = V;
            }
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
        MyContext.valuePtr = 0;
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
        builder.setInsertPoint(BB);
        enterBlock = true;
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
        }
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
        if (enterBlock) enterBlock = false;
        else symbolTable.addLevel();
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

    LinkedList<LinkedList<Instructions.BranchInst>> breakStk = new LinkedList<>();
    LinkedList<LinkedList<Instructions.BranchInst>> continueStk = new LinkedList<>();

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
                visit(ctx.lVal());
                Value lhs = curVal;
                visit(ctx.exp());
                Value rhs = curVal;
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
                    assert breakStk.peek() != null;
                    breakStk.peek().add((Instructions.BranchInst) builder.createBr(null));
                } else if (ctx.CONTINUE() != null) {//continue;
                    assert continueStk.peek() != null;
                    continueStk.peek().add((Instructions.BranchInst) builder.createBr(null));
                } else {//return ';'
                    builder.createRetVoid();
                }
                break;
            case 5:
            case 7:
                if (ctx.IF() != null) {//'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                    Instruction TrueBr,FalseBr,CondBr;
                    BasicBlock trueBlock,falseBlock,stmtBlock;

                    visit(ctx.cond());
                    trueBlock = builder.createBasicBlock(curF);
                    CondBr = builder.createCondBr(curVal, trueBlock, null);
                    builder.setInsertPoint(trueBlock);
                    visit(ctx.stmt(0));
                    TrueBr = builder.createBr(null);
                    if (ctx.ELSE() != null) {
                        falseBlock = builder.createBasicBlock(curF);
                        ((Instructions.BranchInst)CondBr).setIfFalse(falseBlock);
                        builder.setInsertPoint(falseBlock);
                        visit(ctx.stmt(1));
                        FalseBr = builder.createBr(null);

                        stmtBlock = builder.createBasicBlock(curF);
                        ((Instructions.BranchInst)TrueBr).setBr(stmtBlock);
                        ((Instructions.BranchInst)FalseBr).setBr(stmtBlock);
                        builder.setInsertPoint(stmtBlock);
                    }else{
                        stmtBlock = builder.createBasicBlock(curF);
                        ((Instructions.BranchInst)CondBr).setIfFalse(stmtBlock);
                        ((Instructions.BranchInst)TrueBr).setBr(stmtBlock);
                        builder.setInsertPoint(stmtBlock);
                    }
                } else {//WHILE LPAREN  cond RPAREN  stmt
                    Instruction cycBr,CondBr;
                    BasicBlock trueBlock,falseBlock;

                    breakStk.add(new LinkedList<>());//false
                    continueStk.add(new LinkedList<>());//cond

                    //判断
                    BasicBlock condBlock = builder.createBasicBlock(curF);
                    while(!continueStk.peek().isEmpty()){
                        continueStk.peek().pop().setBr(condBlock);
                    }
                    builder.createBr(condBlock);
                    builder.setInsertPoint(condBlock);
                    visit(ctx.cond());
                    trueBlock = builder.createBasicBlock(curF);
                    CondBr = builder.createCondBr(curVal, trueBlock, null);
                    //循环
                    builder.setInsertPoint(trueBlock);
                    visit(ctx.stmt(0));
                    cycBr = builder.createBr(condBlock);
                    //退出循环
                    falseBlock = builder.createBasicBlock(curF);
                    ((Instructions.BranchInst)CondBr).setIfFalse(falseBlock);
                    while(!breakStk.peek().isEmpty()){
                        breakStk.peek().pop().setBr(falseBlock);
                    }
                    builder.setInsertPoint(falseBlock);

                    breakStk.pop();
                    continueStk.pop();
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
        ctx.lOrExp().trueBlock = ctx.trueBlock;
        ctx.lOrExp().falseBlock = ctx.falseBlock;
        visit(ctx.lOrExp());
        if (!curVal.getType().isInt1Ty()) {
            curVal = builder.createCmp(CmpInst.Predicate.ICMP_NE, curVal, Constants.ConstantInt.const_0());
        }
        return null;
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
        curVal = getLVal(curVal, ctx);

        return curVal;
    }

    public Value getLVal(Value V, SysyParser.LValContext ctx) {
        Type ty = V.getType();
        if (ty.isPointerTy()) {
            PointerType pty = (PointerType) ty;
            if (pty.getElementType().isPointerTy()) {
                V = builder.createLoad(V);
                V = getLVal(V, ctx);
            } else if (pty.getElementType().isArrayTy()) {
                for (int i = 0; i < ctx.exp().size(); i++) {
                    visit(ctx.exp(i));
                    V = builder.createGEP(V, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(curVal);
                    }});
                }
//                V = getLVal(V, ctx);
            }
//            else if (pty.getElementType().isIntegerTy() || pty.getElementType().isFloatTy()) {
//                V = builder.createLoad(V);
//            }
        }
        return V;
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
            if (curVal.getType().isPointerTy()) curVal=builder.createLoad(curVal);
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
                LogError("未找到名为" + name + "的函数");
            }
            FunctionType FT = (FunctionType) F.getType();
            if (FT.getContainedTys().size() - 1 != ctx.children.size() - 3) {//参数数量不对
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
            ctx.lAndExp().trueBlock = ctx.trueBlock;
            ctx.lAndExp().falseBlock = ctx.falseBlock;
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
