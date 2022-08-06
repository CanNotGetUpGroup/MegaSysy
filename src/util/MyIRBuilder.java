package util;

import ir.*;
import ir.Module;
import ir.instructions.BinaryInstruction;
import ir.instructions.CastInst;
import ir.instructions.CmpInst;
import ir.instructions.CmpInst.*;
import ir.instructions.Instructions.*;

import java.util.ArrayList;

/**
 * 创建、插入各种Instruction
 */
public class MyIRBuilder {
    private static final MyIRBuilder myIRBuilder = new MyIRBuilder();
    public BasicBlock BB;
    private IListNode<Instruction, BasicBlock> InsertPt;
    private MyContext context = MyContext.getInstance();

    public static MyIRBuilder getInstance() {
        return myIRBuilder;
    }

    public void InsertHelper(Instruction I, String Name, BasicBlock basicBlock, IListNode<Instruction, BasicBlock> insertPt) {
        if (basicBlock != null) {
            //插入到基本块指定位置
            I.getInstNode().insertBefore(insertPt);
        }
        if (Name != null) I.setName(Name);
    }

    public Value insert(Constant C) {
        return C;
    }

    public Value insert(Value V) {
        if (V instanceof Instruction) {
            insert((Instruction) V);
        }
        return V;
    }

    public Instruction insert(Instruction I) {
        InsertHelper(I, null, BB, InsertPt);
        return I;
    }

    public Instruction insert(Instruction I, String name) {
        InsertHelper(I, name, BB, InsertPt);
        return I;
    }

    public void setInsertPoint(BasicBlock b) {
        BB = b;
        InsertPt = b.getInstList().getTail();
    }

    public void setInsertPoint(Instruction I) {
        BB = I.getParent();
        InsertPt = I.getInstNode();
    }

    private void setInsertPt(IListNode<Instruction, BasicBlock> insertPt) {
        InsertPt = insertPt;
    }

    public BasicBlock createBasicBlock(String name, Function parent) {
        BasicBlock BB = BasicBlock.create(name, parent);
        return BB;
    }

    public BasicBlock createBasicBlock( Function parent) {
        BasicBlock BB = BasicBlock.create(parent);
        return BB;
    }

    Value retL,retR;

    /**
     * 类型转换 int1<int32<float
     * @param left 为true表示L优先级低于R，L转换成R的类型，返回retL；为false则相反
     */
    public void unifyType(Value L, Value R, boolean left){
        if(!L.getType().equals(R.getType())){
            if(L.getType().isInt1Ty()){
                if(R.getType().isInt32Ty()){
                    if(left) retL=createZExt(L,Type.getInt32Ty());
                    else retR=createZExt(L,Type.getInt32Ty());
                }else if(R.getType().isFloatTy()){
                    if(left) retL=createSIToFP(L,Type.getFloatTy());
                    else retR=createSIToFP(L,Type.getFloatTy());
                }
            }else if(L.getType().isInt32Ty()){
                if(R.getType().isFloatTy()){
                    if(left) retL=createSIToFP(L,Type.getFloatTy());
                    else retR=createSIToFP(L,Type.getFloatTy());
                }else if(R.getType().isInt1Ty()){
                    unifyType(R,L,false);
                }
            }else if(L.getType().isFloatTy()){
                unifyType(R,L,false);
            }
        }
    }

    /**
     * Instructions factory functions
     */

    /**
     * 新建全局变量
     */
    public GlobalVariable createGlobalVariable(String name,Type ty, Module parent, Constant InitVal, boolean isConstantGlobal) {
        return GlobalVariable.create(name,ty, parent, InitVal, isConstantGlobal);
    }

    /**
     * 新建函数
     */
    public Function createFunction(DerivedTypes.FunctionType type, String name, Module module) {
        return Function.create(type, name, module);
    }

    public Function createFunction(DerivedTypes.FunctionType type, String name, Module module,boolean isDefined) {
        Function tmp=Function.create(type, name, module);
        tmp.setDefined(isDefined);
        return tmp;
    }

    //===--------------------------------------------------------------------===//
    // Intrinsic creation methods
    //===--------------------------------------------------------------------===//

    /// create and insert a memset to the specified pointer and the
    /// specified value.
    ///
    /// If the pointer isn't an i8*, it will be converted. If a TBAA tag is
    /// specified, it will be added to the instruction. Likewise with alias.scope
    /// and noalias tags.

//    CallInst createMemSet(Value Ptr, Value Val, int Size) {
//
//    }

    public Instruction createPHI(Type ty, int block_num){
        return insert(PHIInst.create(ty,block_num));
    }

    public Instruction createPHI(Type ty, int block_num,String Name, Instruction InsertBefore){
        return insert(PHIInst.create(ty,block_num,Name,InsertBefore));
    }

    public Instruction createPHI(Type ty, int block_num,String Name, BasicBlock InsertAtEnd){
        return insert(PHIInst.create(ty,block_num,Name,InsertAtEnd));
    }

    //===--------------------------------------------------------------------===//
    // Instruction creation methods: Terminators
    //===--------------------------------------------------------------------===//

    public Instruction createRet(Value V) {
        return insert(ReturnInst.create(V));
    }

    public Instruction createRetVoid() {
        return insert(ReturnInst.create());
    }

    public Instruction createBr(BasicBlock Dest) {
        return insert(BranchInst.create(Dest));
    }

    // create a conditional 'br Cond, TrueDest, FalseDest'
    // instruction.
    public Instruction createCondBr(Value Cond, BasicBlock True, BasicBlock False) {
        return insert(BranchInst.create(True, False, Cond));
    }

//    public CallBrInst createCallBr(DerivedTypes.FunctionType Ty, Value Callee, BasicBlock DefaultDest,
//                                   ArrayList<BasicBlock> IndirectDests,ArrayList<Value> Args){
//
//    }

    //===--------------------------------------------------------------------===//
    // Instruction creation methods: Binary Operators
    //===--------------------------------------------------------------------===//

    public Value createBinary(Instruction.Ops op,Value L ,Value R){
        // 转换类型
        retL=L;
        retR=R;
        unifyType(L,R,true);
        if(retL.getType().isFloatTy()){
            if(op.equals(Instruction.Ops.Add)){
                op= Instruction.Ops.FAdd;
            }else if(op.equals(Instruction.Ops.Sub)){
                op=Instruction.Ops.FSub;
            }else if(op.equals(Instruction.Ops.Mul)){
                op=Instruction.Ops.FMul;
            }else if(op.equals(Instruction.Ops.SDiv)){
                op=Instruction.Ops.FDiv;
            }else if(op.equals(Instruction.Ops.SRem)){
                op=Instruction.Ops.FRem;
            }
        }
        switch (op){
            case Add:
                return createAdd(retL,retR);
            case Sub:
                return createSub(retL,retR);
            case Mul:
                return createMul(retL,retR);
            case SDiv:
                return createSDiv(retL,retR);
            case SRem:
                return createSRem(retL,retR);
            case FAdd:
                return createFAdd(retL,retR);
            case FSub:
                return createFSub(retL,retR);
            case FMul:
                return createFMul(retL,retR);
            case FDiv:
                return createFDiv(retL,retR);
            case FRem:
                return createFRem(retL,retR);
        }
        return null;
    }

    public Value foldConstant(Instruction.Ops Opc, Value L, Value R) {
        //L和R都不是Constant
        if (!(L instanceof Constant || R instanceof Constant)) {
            return null;
        }
        return insert(Folder.createBinOp(Opc, L, R));
    }

    public Value createAdd(Value LHS, Value RHS) {
        Value ret;
        if (LHS instanceof Constant || RHS instanceof Constant) {
            ret = Folder.createAdd( LHS, RHS);
            if(ret!=null){
                return insert(ret);
            }
        }
        return insert(BinaryInstruction.create(Instruction.Ops.Add,LHS,RHS));
    }

    public Value createSub(Value LHS, Value RHS) {
        Value ret;
        if (LHS instanceof Constant || RHS instanceof Constant) {
            ret = insert(Folder.createSub( LHS,  RHS));
            if(ret!=null){
                return insert(ret);
            }
        }
        return insert(BinaryInstruction.create(Instruction.Ops.Sub,LHS,RHS));
    }

    public Value createMul(Value LHS, Value RHS) {
        Value ret;
        if (LHS instanceof Constant || RHS instanceof Constant) {
            ret = insert(Folder.createMul( LHS,  RHS));
            if(ret!=null){
                return insert(ret);
            }
        }
        return insert(BinaryInstruction.create(Instruction.Ops.Mul,LHS,RHS));
    }

    public Value createSDiv(Value LHS, Value RHS) {
        Value ret;
        if (LHS instanceof Constant || RHS instanceof Constant) {
            ret = insert(Folder.createSDiv( LHS,  RHS));
            if(ret!=null){
                return insert(ret);
            }
        }
        return insert(BinaryInstruction.create(Instruction.Ops.SDiv,LHS,RHS));
    }

    public Value createSRem(Value LHS, Value RHS) {
        //取模的常量折叠只处理了两个都为常量或x%1==0的情况
        Value V = foldConstant(Instruction.Ops.SRem, LHS, RHS);
        if (V != null) {
            return insert(V);
        }
        //其它情况做取模优化，变为x - x / y * y（y为常量且绝对值是2的次幂的情况交给后端处理）
        if(RHS instanceof Constants.ConstantInt){
            Constants.ConstantInt CI2=(Constants.ConstantInt)RHS;
            int abs_num=Math.abs(CI2.getVal());
            if((abs_num&(abs_num-1))==0){//对二的次幂取模，交给后端处理
                return insert(BinaryInstruction.create(Instruction.Ops.SRem,LHS,RHS));
            }
        }
        var div=createSDiv(LHS,RHS);
        var mul=createMul(div,RHS);
        return createSub(LHS,mul);
//        return insert(BinaryInstruction.create(Instruction.Ops.SRem,LHS,RHS));
    }

    public Value createFAdd(Value LHS, Value RHS) {
        Value V = foldConstant(Instruction.Ops.FAdd, LHS, RHS);
        if (V != null) {
            return insert(V);
        }
        return insert(BinaryInstruction.create(Instruction.Ops.FAdd,LHS,RHS));
    }

    public Value createFSub(Value LHS, Value RHS) {
        Value V = foldConstant(Instruction.Ops.FSub, LHS, RHS);
        if (V != null) {
            return insert(V);
        }
        return insert(BinaryInstruction.create(Instruction.Ops.FSub,LHS,RHS));
    }

    public Value createFMul(Value LHS, Value RHS) {
        Value V = foldConstant(Instruction.Ops.FMul, LHS, RHS);
        if (V != null) {
            return insert(V);
        }
        return insert(BinaryInstruction.create(Instruction.Ops.FMul,LHS,RHS));
    }

    public Value createFDiv(Value LHS, Value RHS) {
        Value V = foldConstant(Instruction.Ops.FDiv, LHS, RHS);
        if (V != null) {
            return insert(V);
        }
        return insert(BinaryInstruction.create(Instruction.Ops.FDiv,LHS,RHS));
    }

    public Value createFRem(Value LHS, Value RHS) {
        Value V = foldConstant(Instruction.Ops.FRem, LHS, RHS);
        if (V != null) {
            return insert(V);
        }
        return insert(BinaryInstruction.create(Instruction.Ops.FRem,LHS,RHS));
    }

    public Value createAnd(Value LHS, Value RHS) {
        if (RHS instanceof Constant) {
            // LHS & -1 -> LHS
            if(RHS instanceof Constants.ConstantInt && ((Constants.ConstantInt)RHS).getVal()==-1){
                return LHS;
            }
            if(LHS instanceof Constant){
                return insert(Folder.createAnd((Constant) LHS, (Constant) RHS));
            }
        }
        return insert(BinaryInstruction.create(Instruction.Ops.And,LHS,RHS));
    }

    public Value createOr(Value LHS, Value RHS) {
        if (RHS instanceof Constant) {
            // LHS | 0 -> LHS
            if(RHS instanceof Constants.ConstantInt && ((Constants.ConstantInt)RHS).getVal()==0){
                return LHS;
            }
            if(LHS instanceof Constant){
                return insert(Folder.createOr((Constant) LHS, (Constant) RHS));
            }
        }
        return insert(BinaryInstruction.create(Instruction.Ops.Or,LHS,RHS));
    }

    public Value createLogicalAnd(Value LHS, Value RHS) {
        if(!LHS.getType().isInt1Ty()){
            LHS=createCmp(Predicate.ICMP_NE,LHS, Constants.ConstantInt.const_0());
        }
        if(!RHS.getType().isInt1Ty()){
            RHS=createCmp(Predicate.ICMP_NE,RHS, Constants.ConstantInt.const_0());
        }
        return createSelect(LHS,RHS, Constant.getNullValue(RHS.getType()));
    }

    public Value createLogicalOr(Value LHS, Value RHS) {
        if(!LHS.getType().isInt1Ty()){
            LHS=createCmp(Predicate.ICMP_NE,LHS, Constants.ConstantInt.const_0());
        }
        if(!RHS.getType().isInt1Ty()){
            RHS=createCmp(Predicate.ICMP_NE,RHS, Constants.ConstantInt.const_0());
        }
        return createSelect(LHS, Constant.getAllOnesValue(RHS.getType()),RHS);
    }

    public Value createNot(Value V){
        if(V instanceof Constant){
            return insert(Folder.createNot((Constant)V));
        }
//        return insert(BinaryInstruction.createNot(V));
        return createICmpEQ(V, Constants.ConstantInt.const1_0());
    }

    //===--------------------------------------------------------------------===//
    // Instruction creation methods: Memory Instructions
    // 所有声明的变量放在entry基本块首
    //===--------------------------------------------------------------------===//

    public Instruction createAlloca(Type Ty) {
        Instruction I=new AllocaInst(Ty);
        I.getInstNode().insertAfter(BB.getParent().getEntryBB().getInstList().getHead());
        return I;
    }

    public Value createLoad(Type Ty, Value Ptr) {
        return insert(new LoadInst(Ty, Ptr));
    }

    public Value createLoad(Value Ptr) {
        assert Ptr.getType().isPointerTy();
        return insert(new LoadInst(((DerivedTypes.PointerType)Ptr.getType()).getElementType(), Ptr));
    }

    public Instruction createStore(Value Val, Value Ptr) {
        if(!Val.getType().equals(((DerivedTypes.PointerType)Ptr.getType()).getElementType())){
            if(Val.getType().equals(Type.getInt32Ty())){
                Val=createSIToFP(Val,Type.getFloatTy());
            }else{
                Val=createFPToSI(Val,Type.getInt32Ty());
            }
        }
        return insert(new StoreInst(Val, Ptr));
    }

    public Instruction createGEP(Value Ptr, ArrayList<Value> IdxList) {
        return insert(GetElementPtrInst.create(null, Ptr, IdxList));
    }


    public Instruction createGEP(Type Ty, Value Ptr, ArrayList<Value> IdxList) {
        return insert(GetElementPtrInst.create(Ty, Ptr, IdxList));
    }

    //===--------------------------------------------------------------------===//
    // Instruction creation methods: Cast/Conversion Operators
    //===--------------------------------------------------------------------===//

    /**
     * 若V为常量，则自动进行转换，直接返回转换的结果
     */
    public Value createCast(Instruction.Ops Op, Value V, Type DestTy) {
        if (V.getType().equals(DestTy)) {
            return V;
        }
        if (V instanceof Constant) {
            return insert(Folder.createCast(Op, (Constant) V, DestTy));
        }
        return insert(CastInst.create(Op, V, DestTy));
    }

    public Value createZExt(Value V, Type DestTy) {
        return createCast(Instruction.Ops.ZExt, V, DestTy);
    }

//    Value createFPExt(Value V, Type DestTy) {
//        return createCast(Instruction.CastOps.FPExt, V, DestTy);
//    }

    public Value createFPToSI(Value V, Type DestTy) {
        return createCast(Instruction.Ops.FPToSI, V, DestTy);
    }

    public Value createSIToFP(Value V, Type DestTy) {
        return createCast(Instruction.Ops.SIToFP, V, DestTy);
    }

    public Value createBitCast(Value V, Type DestTy) {
        return insert(BitCastInst.create(V,DestTy));
    }

    //===--------------------------------------------------------------------===//
    // Instruction creation methods: Compare Instructions
    //===--------------------------------------------------------------------===//

    /**
     * 若LHS和RHS都为常量，则经过常量折叠，直接返回比较的结果
     */
    public Value createCmp(Predicate Pred, Value L, Value R) {
        // 类型转换
        retL=L;
        retR=R;
        unifyType(L,R,true);
        if(retL.getType().isFloatTy()){
            if(Pred.equals(Predicate.ICMP_EQ)){
                Pred= Predicate.FCMP_UEQ;
            }else if(Pred.equals(Predicate.ICMP_NE)){
                Pred=Predicate.FCMP_UNE;
            }else if(Pred.equals(Predicate.ICMP_SGE)){
                Pred=Predicate.FCMP_UGE;
            }else if(Pred.equals(Predicate.ICMP_SGT)){
                Pred=Predicate.FCMP_UGT;
            }else if(Pred.equals(Predicate.ICMP_SLE)){
                Pred=Predicate.FCMP_ULE;
            }else if(Pred.equals(Predicate.ICMP_SLT)){
                Pred=Predicate.FCMP_ULT;
            }
        }
        return CmpInst.isFPPredicate(Pred)
                ? createFCmp(Pred, retL, retR)
                : createICmp(Pred, retL, retR);
    }

    public Value createICmp(Predicate P, Value LHS, Value RHS) {
        //常量折叠
        if (LHS instanceof Constant && RHS instanceof Constant) {
            return insert(Folder.createIcmp(P, (Constant) LHS, (Constant) RHS));
        }
        return insert(new ICmpInst(P, LHS, RHS));
    }

    // create a quiet floating-point comparison (i.e. one that raises an FP
    // exception only in the case where an input is a signaling NaN).
    // Note that this differs from createFCmpS only if IsFPConstrained is true.
    public Value createFCmp(Predicate P, Value LHS, Value RHS) {
        if (LHS instanceof Constant && RHS instanceof Constant) {
            return insert(Folder.createFcmp(P, (Constant) LHS, (Constant) RHS));
        }
        return insert(new FCmpInst(P, LHS, RHS));
    }

    public Value createICmpEQ(Value LHS, Value RHS) {
        return createICmp(Predicate.ICMP_EQ, LHS, RHS);
    }

    public Value createICmpNE(Value LHS, Value RHS) {
        return createICmp(Predicate.ICMP_NE, LHS, RHS);
    }

    public Value createICmpSGE(Value LHS, Value RHS) {
        return createICmp(Predicate.ICMP_SGE, LHS, RHS);
    }

    public Value createICmpSGT(Value LHS, Value RHS) {
        return createICmp(Predicate.ICMP_SGT, LHS, RHS);
    }

    public Value createICmpSLE(Value LHS, Value RHS) {
        return createICmp(Predicate.ICMP_SLE, LHS, RHS);
    }

    public Value createICmpSLT(Value LHS, Value RHS) {
        return createICmp(Predicate.ICMP_SLT, LHS, RHS);
    }

    public Value createFCmpUEQ(Value LHS, Value RHS) {
        return createFCmp(Predicate.FCMP_UEQ, LHS, RHS);
    }

    public Value createFCmpUGT(Value LHS, Value RHS) {
        return createFCmp(Predicate.FCMP_UGT, LHS, RHS);
    }

    public Value createFCmpUGE(Value LHS, Value RHS) {
        return createFCmp(Predicate.FCMP_UGE, LHS, RHS);
    }

    public Value createFCmpULT(Value LHS, Value RHS) {
        return createFCmp(Predicate.FCMP_ULT, LHS, RHS);
    }

    public Value createFCmpULE(Value LHS, Value RHS) {
        return createFCmp(Predicate.FCMP_ULE, LHS, RHS);
    }

    public Value createFCmpUNE(Value LHS, Value RHS) {
        return createFCmp(Predicate.FCMP_UNE, LHS, RHS);
    }

    //===--------------------------------------------------------------------===//
    // Instruction creation methods: Other Instructions
    //===--------------------------------------------------------------------===//

    public Instruction createCall(Function F, ArrayList<Value> Args) {
        return insert(CallInst.create(F, Args));
    }

    public Value createSelect(Value C, Value True, Value False) {
        if(C instanceof Constant && True instanceof Constant && False instanceof Constant){
            return insert(Folder.createSelect((Constant) C,(Constant)True,(Constant)False));
        }
        return insert(SelectInst.create(C,True,False));
    }

}
