package frontend;

import backend.machineCode.Instruction.Branch;
import ir.*;
import ir.DerivedTypes.*;
import ir.Constants.*;
import ir.Instruction.*;
import ir.Module;
import ir.instructions.*;
import util.IListNode;
import util.MyIRBuilder;
import util.SymbolTable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

public class Visitor extends SysyBaseVisitor<Value> {
    static MyIRBuilder builder = MyIRBuilder.getInstance();
    MyContext context = MyContext.getInstance();
    SymbolTable symbolTable = SymbolTable.getInstance();
    Module module = Module.getInstance();
    Value curVal;
    Function curF;
    Type type;
    Stack<FunctionType> fType = new Stack<>();
    ArrayType arrayType; //声明的数组类型
    Type declType;
    ArrayList<Type> paramTypes; //参数类型
    Stack<ArrayList<Value>> paramList = new Stack<>(); //参数列表
    boolean enterBlock; //是否进入block，用于函数参数的初始化

    Logger logger = Logger.getLogger(Visitor.class.getName());

    Value LogError(String str) {
        logger.severe(str);
        throw new RuntimeException(str);
    }

    @Override
    public Value visitProgram(SysyParser.ProgramContext ctx) {
        ArrayList<Type> param_array = new ArrayList<>();
        param_array.add(PointerType.get(Type.getInt32Ty()));
        ArrayList<Type> param_farray = new ArrayList<>();
        param_farray.add(PointerType.get(Type.getFloatTy()));
        ArrayList<Type> param_int = new ArrayList<>();
        param_int.add(Type.getInt32Ty());
        ArrayList<Type> param_float = new ArrayList<>();
        param_float.add(Type.getFloatTy());
        ArrayList<Type> param_put_array = new ArrayList<>();
        param_put_array.add(Type.getInt32Ty());
        param_put_array.add(PointerType.get(Type.getInt32Ty()));
        ArrayList<Type> param_put_farray = new ArrayList<>();
        param_put_farray.add(Type.getInt32Ty());
        param_put_farray.add(PointerType.get(Type.getFloatTy()));
        ArrayList<Type> param_memset = new ArrayList<>();
        param_memset.add(PointerType.get(Type.getInt32Ty()));
        param_memset.add(Type.getInt32Ty());
        param_memset.add(Type.getInt32Ty());

        symbolTable.addValue("getint", builder.createFunction(FunctionType.get(Type.getInt32Ty()), "getint", module, false));
        symbolTable.addValue("getfloat", builder.createFunction(FunctionType.get(Type.getFloatTy()), "getfloat", module, false));
        symbolTable.addValue("getarray", builder.createFunction(FunctionType.get(Type.getInt32Ty(), param_array), "getarray", module, false));
        symbolTable.addValue("getfarray", builder.createFunction(FunctionType.get(Type.getInt32Ty(), param_farray), "getfarray", module, false));
        symbolTable.addValue("getch", builder.createFunction(FunctionType.get(Type.getInt32Ty()), "getch", module, false));
        symbolTable.addValue("putint", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_int), "putint", module, false));
        symbolTable.addValue("putch", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_int), "putch", module, false));
        symbolTable.addValue("putfloat", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_float), "putfloat", module, false));
        symbolTable.addValue("putarray", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_put_array), "putarray", module, false));
        symbolTable.addValue("putfarray", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_put_farray), "putfarray", module, false));
        symbolTable.addValue("memset", builder.createFunction(FunctionType.get(Type.getVoidTy(), param_memset), "memset", module, false));
        symbolTable.addValue("starttime", builder.createFunction(FunctionType.get(Type.getVoidTy()), "_sysy_starttime", module, false));
        symbolTable.addValue("stoptime", builder.createFunction(FunctionType.get(Type.getVoidTy()), "_sysy_stoptime", module, false));

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
        declType=type;
        return super.visitBType(ctx);
    }

    public void castType(Type ty){
        if (!curVal.getType().equals(ty)) {
            if(curVal.getType().equals(Type.getInt32Ty())){
                curVal=builder.createSIToFP(curVal,Type.getFloatTy());
            }else{
                curVal=builder.createFPToSI(curVal,Type.getInt32Ty());
            }
        }
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
            arrayType = ArrayType.get(declType, ((ConstantInt) curVal).getVal());
            for (int i = ctx.constExp().size() - 2; i >= 0; i--) {
                visit(ctx.constExp(i));
                if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).getVal() >= 0)) {
                    LogError("数组各维长度应为非负整数");
                }
                arrayType = ArrayType.get(arrayType, ((ConstantInt) curVal).getVal());
            }
            ArrayType Aty;
            if (!symbolTable.inGlobalArea()) {
                Instruction alloca = builder.createAlloca(arrayType);
                alloca.setComment("const "+declType+" "+ctx.getText());
                alloca.setVarName(ctx.IDENT().getText());
                symbolTable.addValue(name, alloca);
                Type tmpTy = arrayType.getKidType();
                ((Instructions.AllocaInst) alloca).setUndef(false);
                //利用memset初始化数组为0
                gep = builder.createGEP(alloca, new ArrayList<>() {{
                    add(ConstantInt.const_0());
                    add(ConstantInt.const_0());
                }});
                arrInst=new ArrayList<>();
                arrInstIdx=new ArrayList<>();
                arrInst.add(gep);
                arrInstIdx.add(0);
                while (tmpTy.isArrayTy()) {
                    gep = builder.createGEP(gep, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(ConstantInt.const_0());
                    }});
                    arrInst.add(gep);
                    arrInstIdx.add(0);
                    if (((ArrayType) tmpTy).getKidType().isArrayTy()) tmpTy = ((ArrayType) tmpTy).getKidType();
                    else break;
                }
                ArrayList<Value> Args = new ArrayList<>();
                Args.add(gep);
                Args.add(Constant.getNullValue(Type.getInt32Ty()));
                Args.add(ConstantInt.get(arrayType.getNumElements() * arrayType.getEleSize() * 4));
                //TODO: 浮点数组初始化
//                builder.createBitCast(alloca,)
                Value v = builder.createCall(symbolTable.getFunction("memset"), Args);
                v.setComment(name);
                gep=alloca;
            }

            typeArrayList = new ArrayList<>();
            Aty = arrayType;
            while (Aty.isArrayTy()) {
                typeArrayList.add(Aty);
                if (Aty.getKidType().isArrayTy()) Aty = (ArrayType) Aty.getKidType();
                else break;
            }
            tmpArrList = new ArrayList<>();
            tmpIdx = new ArrayList<>();
            arr_site=0;
            after_site=0;
            getDimInfo = false;
            visit(ctx.constInitVal());
            getDimInfo = true;
            visit(ctx.constInitVal());
//            //int a[2][2]={1,2,3}，生成的tmpArrList中只有{1,2,3}，补齐0
//            while (arr_site < arrayType.getNumElements() * arrayType.getEleSize()) {
//                tmpArrList.put(arr_site++, Constant.getNullValue(declType));
//            }
            curVal = changeArrType(arrayType, tmpArrList, tmpIdx);

            if (symbolTable.inGlobalArea()) {
                curVal = builder.createGlobalVariable("@" + name, arrayType, module, (Constant) curVal, true);
                symbolTable.addValue(name, curVal);
            }
        } else {
            visit(ctx.constInitVal());
            castType(declType);
            symbolTable.addValue(name, curVal);
        }
        return curVal;
    }

    ArrayList<ArrayType> typeArrayList;
    Instruction gep;
    ArrayList<Integer> tmpIdx = null;
    ArrayList<Value> tmpArrList = null;
    int arr_site=0;
    int after_site=0;
    ArrayList<Instruction> arrInst; //a->a[0]->a[0][0]
    ArrayList<Integer> arrInstIdx;

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
                int pre=arr_site;
                ArrayList<Value> V = new ArrayList<>();
                ArrayList<Integer> Idx=new ArrayList<>();
                for (int i = 0; i < ctx.constInitVal().size(); i++) {
                    if (ctx.constInitVal(i).children.size() == 1) {
                        visit(ctx.constInitVal(i));
                        if (!declType.equals(curVal.getType()) && !(declType.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty()))) {
                            LogError("赋值类型与定义不符");
                        }
                        //整数转浮点数
                        if (declType.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty())) {
                            curVal = builder.createCast(Ops.SIToFP, curVal, Type.getFloatTy());
                        }
                        /*  a[4][3][2]  arr_site=23
                         arrInstIdx  new             ptr
                             0       23/6=3 23%6=5   gep arrInst.get(0) 3
                             0       5/2=1  5%2=1    gep ptr 0 1
                             0       1/1=1           gep ptr 0 1
                        */
                        if (!symbolTable.inGlobalArea()) {
                            // 初始化时全赋为0，因此此处不需要再赋值
                            if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).isZero())) {
                                Integer idx;
                                int idx_site=arr_site;
                                Instruction ptr = arrInst.get(arrayType.getDim()-1);
                                boolean flag=false;
                                for(int j=0;j<arrayType.getDim();j++){
                                    ArrayType arrayType1=typeArrayList.get(j);
                                    idx=idx_site/(arrayType1.getEleSize());
                                    if(flag||!arrInstIdx.get(j).equals(idx)){
                                        Integer finalDel = idx-arrInstIdx.get(j);
                                        Integer finalIdx = idx;
                                        if(!flag){
                                            ptr=builder.createGEP(arrInst.get(j), new ArrayList<>() {{
                                                add(ConstantInt.get(finalDel));
                                            }});
                                            flag=true;
                                        }else{
                                            ptr=builder.createGEP(ptr, new ArrayList<>() {{
                                                add(ConstantInt.get(0));
                                                add(ConstantInt.get(finalIdx));
                                            }});
                                        }
                                        arrInstIdx.set(j,idx);
                                        arrInst.set(j,ptr);
                                    }
                                    idx_site=arr_site%arrayType1.getEleSize();
                                }
                                builder.createStore(curVal, ptr);
                            }
                        }
                        V.add(curVal);
                        Idx.add(arr_site++);
                    } else {
                        //a[2][2][2]={1,{2,3}}={{{1,0},{2,3}},{{0,0},{0,0}}}
                        //a[2][2][2]={1,{{2,3}}}={{{1,0},{0,0}},{{2,3},{0,0}}
                        arr_site+=(eleSize-(arr_site-pre)%eleSize)%eleSize;
                        visit(ctx.constInitVal(i));
                        V.addAll(tmpArrList);
                        Idx.addAll(tmpIdx);
                    }
                }
                arr_site=numEle * eleSize+pre;
                tmpArrList = V;
                tmpIdx = Idx;
            }
        }
        return curVal;
    }

    public ConstantArray changeArrType(ArrayType target, ArrayList<Value> V, ArrayList<Integer> Idx) {
        if(Idx.isEmpty()) return (ConstantArray) Constant.getNullValue(target);
        ArrayList<Value> ret = new ArrayList<>();
        int eleSize = target.getEleSize();
        int numEle = target.getNumElements();
        if (target.getKidType().isArrayTy()) {
            int j=0;
            for (int i = 0; i < numEle && j<Idx.size(); i++) {
                int start=j,end=j;
                for(;j<Idx.size()&&Idx.get(j)>=i*eleSize&&Idx.get(j)<(i+1)*eleSize;j++){
                    end=j+1;
                }
                ret.add(changeArrType((ArrayType) target.getKidType(), new ArrayList<>(V.subList(start, end)),new ArrayList<>(Idx.subList(start, end))));
            }
        } else {
            ret.addAll(V);
            while(ret.size()<numEle){
                ret.add(Constant.getNullValue(target.getKidType()));
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
            arrayType = ArrayType.get(declType, ((ConstantInt) curVal).getVal());
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
                    Instruction alloca = builder.createAlloca(arrayType);
                    alloca.setComment(declType+" "+ctx.getText());
                    alloca.setVarName(ctx.IDENT().getText());
                    symbolTable.addValue(name, alloca);
                    Type tmpTy = arrayType.getKidType();
                    ((Instructions.AllocaInst) alloca).setUndef(false);
                    //利用memset初始化数组为0
                    gep = builder.createGEP(alloca, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(ConstantInt.const_0());
                    }});
                    arrInst=new ArrayList<>();
                    arrInstIdx=new ArrayList<>();
                    arrInst.add(gep);
                    arrInstIdx.add(0);
                    while (tmpTy.isArrayTy()) {
                        gep = builder.createGEP(gep, new ArrayList<>() {{
                            add(ConstantInt.const_0());
                            add(ConstantInt.const_0());
                        }});
                        arrInst.add(gep);
                        arrInstIdx.add(0);
                        if (((ArrayType) tmpTy).getKidType().isArrayTy()) tmpTy = ((ArrayType) tmpTy).getKidType();
                        else break;
                    }
                    ArrayList<Value> Args = new ArrayList<>();
                    Args.add(gep);
                    Args.add(Constant.getNullValue(Type.getInt32Ty()));
                    Args.add(ConstantInt.get(arrayType.getNumElements() * arrayType.getEleSize() * 4));
                    Value v=builder.createCall(symbolTable.getFunction("memset"), Args);
                    v.setComment(name);
                    gep=alloca;
                }

                typeArrayList = new ArrayList<>();
                Aty = arrayType;
                while (Aty.isArrayTy()) {
                    typeArrayList.add(Aty);
                    if (Aty.getKidType().isArrayTy()) Aty = (ArrayType) Aty.getKidType();
                    else break;
                }
                tmpArrList = new ArrayList<>();
                tmpIdx = new ArrayList<>();
                arr_site=0;
                after_site=0;
                getDimInfo = false;
                visit(ctx.initVal());
                getDimInfo = true;
                visit(ctx.initVal());
//                //int a[2][2]={1,2,3}，生成的tmpArrList中只有{1,2,3}，补齐0
//                while (arr_site < arrayType.getNumElements() * arrayType.getEleSize()) {
//                    tmpArrList.put(arr_site++, Constant.getNullValue(declType));
//                }
                if (symbolTable.inGlobalArea()) {
                    curVal = changeArrType(arrayType, tmpArrList, tmpIdx);//全局数组初始化一定是常量
                    curVal = builder.createGlobalVariable("@" + name, arrayType, module, (Constant) curVal, false);
                    symbolTable.addValue(name, curVal);
                }
            } else {
                if (symbolTable.inGlobalArea()) {
                    curVal = builder.createGlobalVariable("@" + name, arrayType, module, Constant.getNullValue(arrayType), false);
                    symbolTable.addValue(name, curVal);
                } else {
                    Value alloca = builder.createAlloca(arrayType);
                    alloca.setComment(declType+" "+ctx.getText());
                    alloca.setVarName(ctx.IDENT().getText());
                    symbolTable.addValue(name, alloca);
                }
            }
        } else {
            if (ctx.initVal() != null) {
                if (symbolTable.inGlobalArea()) {
                    visit(ctx.initVal());
                    castType(declType);
                    Value val = builder.createGlobalVariable("@" + name, declType, module, (Constant) curVal, false);
                    symbolTable.addValue(name, val);
                } else {
                    Value alloca = builder.createAlloca(declType);
                    alloca.setComment(declType+" "+ctx.getText());
                    alloca.setVarName(ctx.IDENT().getText());
                    symbolTable.addValue(name, alloca);
                    visit(ctx.initVal());
                    castType(declType);
                    Value v=builder.createStore(curVal, alloca);
                    v.setComment(name+"="+curVal);
                    ((Instructions.AllocaInst) alloca).setUndef(false);
                }
            } else {
                if (symbolTable.inGlobalArea()) {
                    Value val = builder.createGlobalVariable("@" + name, declType, module, Constant.getNullValue(type), false);
                    symbolTable.addValue(name, val);
                } else {
                    Value alloca = builder.createAlloca(declType);
                    alloca.setComment(declType+" "+ctx.getText());
                    alloca.setVarName(ctx.IDENT().getText());
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
                int pre=arr_site;
                ArrayList<Value> V = new ArrayList<>();
                ArrayList<Integer> Idx = new ArrayList<>();
                for (int i = 0; i < ctx.initVal().size(); i++) {
                    if (ctx.initVal(i).children.size() == 1) {
                        visit(ctx.initVal(i));
                        if (!declType.equals(curVal.getType()) && !(declType.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty()))) {
                            LogError("赋值类型与定义不符");
                        }
                        //整数转浮点数
                        if (declType.equals(Type.getFloatTy()) && curVal.getType().equals(Type.getInt32Ty())) {
                            curVal = builder.createCast(Ops.SIToFP, curVal, Type.getFloatTy());
                        }
                        /*  a[4][3][2]  arr_site=23
                         arrInstIdx  new             ptr
                             0       23/6=3 23%6=5   gep arrInst.get(0) 3
                             0       5/2=1  5%2=1    gep ptr 0 1
                             0       1/1=1           gep ptr 0 1
                        */
                        if (!symbolTable.inGlobalArea()) {
                            // 初始化时全赋为0，因此此处不需要再赋值
                            if (!(curVal instanceof ConstantInt && ((ConstantInt) curVal).isZero())) {
                                Integer idx;
                                int idx_site=arr_site;
                                Instruction ptr = arrInst.get(arrayType.getDim()-1);
                                boolean flag=false;
                                for(int j=0;j<arrayType.getDim();j++){
                                    ArrayType arrayType1=typeArrayList.get(j);
                                    idx=idx_site/(arrayType1.getEleSize());
                                    if(flag||!arrInstIdx.get(j).equals(idx)){
                                        Integer finalDel = idx-arrInstIdx.get(j);
                                        Integer finalIdx = idx;
                                        if(!flag){
                                            ptr=builder.createGEP(arrInst.get(j), new ArrayList<>() {{
                                                add(ConstantInt.get(finalDel));
                                            }});
                                            flag=true;
                                        }else{
                                            ptr=builder.createGEP(ptr, new ArrayList<>() {{
                                                add(ConstantInt.get(0));
                                                add(ConstantInt.get(finalIdx));
                                            }});
                                        }
                                        arrInstIdx.set(j,idx);
                                        arrInst.set(j,ptr);
                                    }
                                    idx_site=arr_site%arrayType1.getEleSize();
                                }
                                builder.createStore(curVal, ptr);
                            }
                        }
                        V.add(curVal);
                        Idx.add(arr_site++);
                    } else {
                        //a[2][2][2]={1,{2,3}}={{{1,0},{2,3}},{{0,0},{0,0}}}
                        //a[2][2][2]={1,{{2,3}}}={{{1,0},{0,0}},{{2,3},{0,0}}}
                        arr_site+=(eleSize-(arr_site-pre)%eleSize)%eleSize;
                        visit(ctx.initVal(i));
                        V.addAll(tmpArrList);
                        Idx.addAll(tmpIdx);
                    }
                }
                arr_site=numEle * eleSize+pre;
                tmpArrList = V;
                tmpIdx = Idx;
            }
        }
        return curVal;
    }

    Instruction retAlloca=null;
    Stack<Instruction> retBrStack=null;

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
        curF.setEntryBB(BB);
        builder.setInsertPoint(BB);
        retAlloca=null;
        if(!FT.getReturnType().isVoidTy()){
            retAlloca=builder.createAlloca(FT.getReturnType());
            retAlloca.setName("ret");
            retAlloca.setVarName("ret");
        }
        retBrStack=new Stack<>();
        enterBlock = true;
        if (ctx.funcFParams() != null) {
            visit(ctx.funcFParams());
        }
        visit(ctx.block());
        BasicBlock endBB = curF.getBbList().getLast().getVal();
        IListNode<Instruction, BasicBlock> endInst = endBB.getInstList().getLast();
        if (endInst == null || !(endInst.getVal() instanceof Instructions.ReturnInst)) {
            retBrStack.push(builder.createBr(null));
        }
        BasicBlock retBB=builder.createBasicBlock("ret",curF);
        while(!retBrStack.isEmpty()){
            ((Instructions.BranchInst)retBrStack.pop()).setBr(retBB);
        }
        builder.setInsertPoint(retBB);
        if(retAlloca!=null) {
            var loadRet=builder.createLoad(retAlloca);
            builder.createRet(loadRet);
        }else{
            builder.createRet(null);
        }
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
            alloc.setComment(type+" "+name);
            alloc.setVarName(ctx.IDENT().getText());
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

    Stack<Stack<Instructions.BranchInst>> breakStk = new Stack<>();     //break回溯
    Stack<Stack<Instructions.BranchInst>> continueStk = new Stack<>();  //continue回溯

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
                builder.createStore(rhs, lhs).setComment(ctx.lVal().getText()+"="+ctx.exp().getText());
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
                    Instruction brRetBB=builder.createBr(null);
                    retBrStack.push(brRetBB);
                }
                break;
            case 5:
            case 7:
                if (ctx.IF() != null) {//'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                    Instruction Stmt0Br, Stmt1Br, CondBr;
                    BasicBlock stmt0Block, stmt1Block, stmtBlock;

                    visit(ctx.cond());
                    stmt0Block = builder.createBasicBlock(curF);
                    stmt0Block.setComment("if Body");
                    CondBr = builder.createCondBr(curVal, stmt0Block, null);
                    CondBr.setComment("judge "+ctx.cond().getText());
                    builder.setInsertPoint(stmt0Block);
                    visit(ctx.stmt(0));
                    Stmt0Br = builder.createBr(null);
                    if (ctx.ELSE() != null) {
                        stmt1Block = builder.createBasicBlock(curF);
                        stmt1Block.setComment("else Body");
                        ((Instructions.BranchInst) CondBr).setIfFalse(stmt1Block);
                        builder.setInsertPoint(stmt1Block);
                        visit(ctx.stmt(1));
                        Stmt1Br = builder.createBr(null);

                        stmtBlock = builder.createBasicBlock(curF);
                        ((Instructions.BranchInst) Stmt0Br).setBr(stmtBlock);
                        ((Instructions.BranchInst) Stmt1Br).setBr(stmtBlock);
                        builder.setInsertPoint(stmtBlock);
                        for(Instructions.BranchInst br:ctx.cond().falseBrs) {
                            br.setIfFalse(stmt1Block);
                        }
                    } else {
                        stmtBlock = builder.createBasicBlock(curF);
                        ((Instructions.BranchInst) CondBr).setIfFalse(stmtBlock);
                        ((Instructions.BranchInst) Stmt0Br).setBr(stmtBlock);
                        builder.setInsertPoint(stmtBlock);
                        for(Instructions.BranchInst br:ctx.cond().falseBrs) {
                            br.setIfFalse(stmtBlock);
                        }
                    }
                    for(Instructions.BranchInst br:ctx.cond().trueBrs) {
                        br.setIfTrue(stmt0Block);
                    }
                } else {//WHILE LPAREN  cond RPAREN  stmt
                    Instruction cycBr, CondBr;
                    BasicBlock trueBlock, falseBlock;

                    breakStk.add(new Stack<>());//false
                    continueStk.add(new Stack<>());//cond

                    //判断
                    BasicBlock condBlock = builder.createBasicBlock(curF);
                    condBlock.setComment("judge "+ctx.cond().getText());
                    builder.createBr(condBlock);
                    builder.setInsertPoint(condBlock);
                    visit(ctx.cond());
                    trueBlock = builder.createBasicBlock(curF);
                    trueBlock.setComment("while Body");
                    CondBr = builder.createCondBr(curVal, trueBlock, null);
                    //循环
                    builder.setInsertPoint(trueBlock);
                    visit(ctx.stmt(0));
                    BasicBlock cycBB=builder.createBasicBlock(curF);
                    cycBB.setComment("jump to head");
                    cycBr = builder.createBr(cycBB);
                    builder.setInsertPoint(cycBB);
                    builder.createBr(condBlock);
                    //退出循环
                    falseBlock = builder.createBasicBlock(curF);
                    falseBlock.setComment("exit while");
                    ((Instructions.BranchInst) CondBr).setIfFalse(falseBlock);
                    while (!continueStk.peek().isEmpty()) {
                        continueStk.peek().pop().setBr(cycBB);
                    }
                    while (!breakStk.peek().isEmpty()) {
                        breakStk.peek().pop().setBr(falseBlock);
                    }
                    builder.setInsertPoint(falseBlock);

                    breakStk.pop();
                    continueStk.pop();

                    for(Instructions.BranchInst br:ctx.cond().trueBrs) {
                        br.setIfTrue(trueBlock);
                    }
                    for(Instructions.BranchInst br:ctx.cond().falseBrs) {
                        br.setIfFalse(falseBlock);
                    }
                }
                break;
            case 3://'return' Exp ';'
                visit(ctx.exp());
                castType(curF.getRetType());
                if(retAlloca!=null) builder.createStore(curVal,retAlloca);
                Instruction brRetBB=builder.createBr(null);
                retBrStack.push(brRetBB);
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
        visit(ctx.lOrExp());
        ctx.trueBrs = ctx.lOrExp().trueBrs;
        ctx.falseBrs = ctx.lOrExp().falseBrs;
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
            if (pty.getElementType().isPointerTy() && ctx.exp().size() != 0) {
                V = builder.createLoad(V);
                visit(ctx.exp(0));
                V = builder.createGEP(V, new ArrayList<>() {{
                    add(curVal);
                }});
                for (int i = 1; i < ctx.exp().size(); i++) {
                    visit(ctx.exp(i));
                    V = builder.createGEP(V, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(curVal);
                    }});
                }
            } else if (pty.getElementType().isArrayTy()) {
                for (int i = 0; i < ctx.exp().size(); i++) {
                    visit(ctx.exp(i));
                    V = builder.createGEP(V, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(curVal);
                    }});
                }
//                V = getLVal(V, ctx);
            } else if (pty.getElementType().isFloatTy() || pty.getElementType().isIntegerTy()) {
                if (ctx.exp().size() > 1) {
                    LogError("维数不对");
                } else if (ctx.exp().size() == 1) {
                    visit(ctx.exp(0));
                    V = builder.createGEP(V, new ArrayList<>() {{
                        add(curVal);
                    }});
                }
            }
//            else if (pty.getElementType().isIntegerTy() || pty.getElementType().isFloatTy()) {
//                V = builder.createLoad(V);
//            }
        }
//        V.setComment("get "+ctx.getText());
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
            if (curVal.getType().isPointerTy()) {
                PointerType pty = (PointerType) curVal.getType();
                if (pty.getElementType().isArrayTy()) {
                    curVal = builder.createGEP(curVal, new ArrayList<>() {{
                        add(ConstantInt.const_0());
                        add(ConstantInt.const_0());
                    }});
                } else curVal = builder.createLoad(curVal);
            }
        } else {
            visit(ctx.number());
        }
        return curVal;
    }

    public int state=0;//用于表示UnaryOp的状态

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
                if(state==0) state=1;//-a
                else if(state==1) state=0;//--a
                else{//-!a
                    addUnaryOpInst(state,tmp,1);
                }
            } else if (ctx.unaryOp().EXC() != null) {
                if(state==0||state==1){
                    addUnaryOpInst(state,tmp,2);
                }
                else if(state==2) state=0;//!!a
            }
            curVal.setComment(ctx.getText());
        } else {//Ident '('[FuncRParams]')'
            String name = ctx.IDENT().getText();
            Function F = symbolTable.getFunction(name);

            if (F == null) {
                LogError("未找到名为" + name + "的函数");
            }
            FunctionType FT = (FunctionType) F.getType();
            int argNum = 0;
            if (ctx.funcRParams() != null) {
                argNum = ctx.funcRParams().exp().size();
            }
            if (FT.getContainedTys().size() - 1 != argNum) {//参数数量不对
                LogError("函数" + symbolTable.getName(F) + "参数数量不对");
            }
            paramList.add(new ArrayList<>());
            fType.add(FT);
            if (ctx.funcRParams() != null) {//F(a,...)
                visit(ctx.funcRParams());
            }
            fType.pop();
            curVal = builder.createCall(F, paramList.pop());
            curVal.setComment("call "+ctx.getText());
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
        for (int i = 0; i < ctx.exp().size(); i++) {
            visit(ctx.exp(i));
            castType(fType.peek().getContainedTys().get(i + 1));
            assert paramList.peek() != null;
            paramList.peek().add(curVal);
        }
        return null;
    }

    public Value addUnaryOpInst(int state,Value L,int after_site){
        if(state==1){
            if (L.getType().isInt1Ty()) {
                L = builder.createZExt(L, Type.getInt32Ty());
            }
            if (L.getType().isInt32Ty()) {
                curVal = builder.createSub(ConstantInt.const_0(), L);
            } else if (L.getType().isFloatTy()) {
                curVal = builder.createFSub(ConstantFP.const_0(), L);
            }
        }else if(state==2){
            if (L.getType().isInt32Ty()) {
                curVal = builder.createICmpEQ(L, ConstantInt.const_0());
            } else if (L.getType().isFloatTy()) {
                curVal = builder.createFCmpUEQ(L, ConstantFP.const_0());
            } else if (L.getType().isInt1Ty()) {
                curVal = builder.createNot(L);
            }
        }
        this.state=after_site;
        return curVal;
    }

    /**
     * mulExp  : unaryExp ((MUL | DIV | MOD) unaryExp)*;
     */
    @Override
    public Value visitMulExp(SysyParser.MulExpContext ctx) {
        Value L = visit(ctx.unaryExp(0)), R;
        L=addUnaryOpInst(state,L,0);
        String comment;
        comment=ctx.unaryExp(0).getText();
        for (int i = 1; i < ctx.unaryExp().size(); i++) {
            R = visit(ctx.unaryExp(i));
            R=addUnaryOpInst(state,R,0);
            comment+=ctx.mulOp(i-1).getText()+ctx.unaryExp(i).getText();
            if (ctx.mulOp(i - 1).MUL() != null) {
                L = builder.createBinary(Ops.Mul, L, R);
            } else if (ctx.mulOp(i - 1).DIV() != null) {
                L = builder.createBinary(Ops.SDiv, L, R);
            } else if (ctx.mulOp(i - 1).MOD() != null) {
                L = builder.createBinary(Ops.SRem, L, R);

            }
            L.setComment(comment);
            curVal = L;
        }
        return curVal;
    }

    /**
     * addExp  : mulExp ((ADD | SUB) mulExp)*;
     */
    @Override
    public Value visitAddExp(SysyParser.AddExpContext ctx) {
        Value L = visit(ctx.mulExp(0)), R;
        String comment;
        comment=ctx.mulExp(0).getText();
        for (int i = 1; i < ctx.mulExp().size(); i++) {
            R = visit(ctx.mulExp(i));
            comment+=ctx.addOp(i-1).getText()+ctx.mulExp(i).getText();
            if (ctx.addOp(i - 1).ADD() != null) {
                L = builder.createBinary(Ops.Add, L, R);
            } else if (ctx.addOp(i - 1).SUB() != null) {
                L = builder.createBinary(Ops.Sub, L, R);
            }
            L.setComment(comment);
            curVal = L;
        }
        return curVal;
    }

    /**
     * relExp  : addExp ((SLT | SGT | SLE | SGE) addExp)*;
     */
    @Override
    public Value visitRelExp(SysyParser.RelExpContext ctx) {
        Value L = visit(ctx.addExp(0)), R;
        String comment;
        comment=ctx.addExp(0).getText();
        for (int i = 1; i < ctx.addExp().size(); i++) {
            R = visit(ctx.addExp(i));
            comment+=ctx.relOp(i-1).getText()+ctx.addExp(i).getText();
            if (ctx.relOp(i - 1).SLT() != null) {
                L = builder.createCmp(CmpInst.Predicate.ICMP_SLT, L, R);
            } else if (ctx.relOp(i - 1).SGT() != null) {
                L = builder.createCmp(CmpInst.Predicate.ICMP_SGT, L, R);
            } else if (ctx.relOp(i - 1).SLE() != null) {
                L = builder.createCmp(CmpInst.Predicate.ICMP_SLE, L, R);
            } else if (ctx.relOp(i - 1).SGE() != null) {
                L = builder.createCmp(CmpInst.Predicate.ICMP_SGE, L, R);
            }
            L.setComment(comment);
            curVal = L;
        }
        return curVal;
    }

    /**
     * eqExp : relExp ((EEQ | UEQ)  relExp)*;
     */
    @Override
    public Value visitEqExp(SysyParser.EqExpContext ctx) {
        Value L = visit(ctx.relExp(0)), R;
        String comment;
        comment=ctx.relExp(0).getText();
        for (int i = 1; i < ctx.relExp().size(); i++) {
            R = visit(ctx.relExp(i));
            comment+=ctx.eqOp(i-1).getText()+ctx.relExp(i).getText();
            if (ctx.eqOp(i - 1).EEQ() != null) {
                L = builder.createCmp(CmpInst.Predicate.ICMP_EQ, L, R);
            } else if (ctx.eqOp(i - 1).UEQ() != null) {
                L = builder.createCmp(CmpInst.Predicate.ICMP_NE, L, R);
            }
            L.setComment(comment);
            curVal = L;
        }
        return curVal;
    }

    /**
     * lAndExp : eqExp (AND eqExp)*;
     */
    @Override
    public Value visitLAndExp(SysyParser.LAndExpContext ctx) {
        Value L = visit(ctx.eqExp(0)), R;
        String comment;
        comment=ctx.eqExp(0).getText();
        ctx.trueBrs=new ArrayList<>();
        ctx.falseBrs=new ArrayList<>();
        for (int i = 1; i < ctx.eqExp().size(); i++) {
            BasicBlock LB=builder.createBasicBlock(curF);
            if(!L.getType().isInt1Ty()){
                L=builder.createCmp(CmpInst.Predicate.ICMP_NE,L, Constants.ConstantInt.const_0());
            }
            Instruction lBr=builder.createCondBr(L,LB,null);
            ctx.falseBrs.add((Instructions.BranchInst) lBr);
            builder.setInsertPoint(LB);
            L = visit(ctx.eqExp(i));
            comment+=" && "+ctx.eqExp(i).getText();
//            if(!R.getType().isInt1Ty()){
//                R=builder.createCmp(CmpInst.Predicate.ICMP_NE,R, Constants.ConstantInt.const_0());
//            }
            L.setComment(comment);
            curVal = L;
        }
        return curVal;
    }

    /**
     * lOrExp  : lAndExp (OR lAndExp)*;
     */
    @Override
    public Value visitLOrExp(SysyParser.LOrExpContext ctx) {
        Value L = visit(ctx.lAndExp(0)), R;
        String comment;
        comment=ctx.lAndExp(0).getText();
        ctx.trueBrs=new ArrayList<>();
        ctx.falseBrs=ctx.lAndExp(0).falseBrs;
        for (int i = 1; i < ctx.lAndExp().size(); i++) {
            BasicBlock LB=builder.createBasicBlock(curF);
            if(!L.getType().isInt1Ty()){
                L=builder.createCmp(CmpInst.Predicate.ICMP_NE,L, Constants.ConstantInt.const_0());
            }
            Instruction lBr=builder.createCondBr(L,null,LB);
            ctx.trueBrs.add((Instructions.BranchInst) lBr);
            for(Instructions.BranchInst br:ctx.falseBrs){
                br.setIfFalse(LB);
            }
            builder.setInsertPoint(LB);
            L = visit(ctx.lAndExp(i));
            ctx.falseBrs=ctx.lAndExp(i).falseBrs;
            comment+=" || "+ctx.lAndExp(i).getText();
//            if(!R.getType().isInt1Ty()){
//                R=builder.createCmp(CmpInst.Predicate.ICMP_NE,R, Constants.ConstantInt.const_0());
//            }
            L.setComment(comment);
            curVal = L;
        }
        return curVal;
    }

    /**
     * number : iNT_CONST|fLOAT_CONST;
     */
    @Override
    public Value visitNumber(SysyParser.NumberContext ctx) {
        return super.visitNumber(ctx);
    }

    @Override
    public Value visitINT_CONST(SysyParser.INT_CONSTContext ctx) {
        type = Type.getInt32Ty();
        if (ctx.DEC_INT_CONST() != null) {
            String text = ctx.DEC_INT_CONST().getText();
            int number = Integer.parseInt(text);
            curVal = ConstantInt.get(number);
        } else if (ctx.OCT_INT_CONST() != null) {
            String text = ctx.OCT_INT_CONST().getText();
            int number = new BigInteger(text, 8).intValue();
            curVal = ConstantInt.get(number);
        } else {
            String text = ctx.HEX_INT_CONST().getText();
            int number = new BigInteger(text.substring(2), 16).intValue();
            curVal = ConstantInt.get(number);
        }
        return curVal;
    }

    @Override
    public Value visitFLOAT_CONST(SysyParser.FLOAT_CONSTContext ctx) {
        type = Type.getFloatTy();
        if (ctx.Decimal_floating_constant() != null) {
            String text = ctx.Decimal_floating_constant().getText();
            float number = Float.parseFloat(text);
            curVal = ConstantFP.get(number);
        } else {
            String text = ctx.Hexadecimal_floating_constant().getText();
            float number = Float.parseFloat(text);
            curVal = ConstantFP.get(number);
        }
        return curVal;
    }
}
