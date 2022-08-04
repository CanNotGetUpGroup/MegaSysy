package ir.instructions;

import analysis.AliasAnalysis;
import analysis.PointerInfo;
import ir.Value;
import ir.*;
import ir.DerivedTypes.*;
import org.antlr.v4.runtime.misc.Pair;
import util.CloneMap;
import util.Folder;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Instructions {
    //===----------------------------------------------------------------------===//
    //                                AllocaInst Class
    //===----------------------------------------------------------------------===//

    /// 在堆栈上分配局部变量的指令
    public static class AllocaInst extends UnaryInstruction {
        private Type AllocatedType;
        private boolean undef = true;

        public ArrayList<BasicBlock> definingBlocks = new ArrayList<>(); //store的基本块
        public ArrayList<BasicBlock> usingBlocks = new ArrayList<>(); //load的基本块
        public StoreInst onlyStore;
        public boolean onlyUsedInOne;
        public BasicBlock onlyBlock;

        /**
         * 申请addr，储存type类型的val
         *
         * @param type 储存Value的类型
         */
        public AllocaInst(Type type) {
            super(DerivedTypes.PointerType.get(type), Ops.Alloca, null);
            AllocatedType = type;
        }

        public void resetAnalyzeInfo() {
            definingBlocks.clear();
            usingBlocks.clear();
            onlyStore = null;
            onlyUsedInOne = true;
            onlyBlock = null;
        }

        @Override
        public String toString() {
            return getName() + " = alloca " + AllocatedType;
        }

        @Override
        public PointerType getType() {
            return (PointerType) super.getType();
        }

        public boolean isUndef() {
            return undef;
        }

        public void setUndef(boolean undef) {
            this.undef = undef;
        }

        public Type getAllocatedType() {
            return AllocatedType;
        }

        public void setAllocatedType(Type allocatedType) {
            AllocatedType = allocatedType;
        }

        @Override
        public AllocaInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (AllocaInst) cloneMap.get(this);
            }
            AllocaInst ret = new AllocaInst(getAllocatedType());
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                                LoadInst Class
    //===----------------------------------------------------------------------===//

    /// 用于访问分配在栈帧的变量的指令
    public static class LoadInst extends UnaryInstruction {
        /**
         * load从addr中读取储存的val
         *
         * @param type 取得Value的类型
         * @param addr 对应的alloca指令
         */
        public LoadInst(Type type, Value addr) {
            super(type, Ops.Load, addr);
            assert type.equals(((DerivedTypes.PointerType) addr.getType()).getElementType());
        }

        @Override
        public String toString() {
            return getName() + " = load " + getType() + ", " +
                    getOperand(0).getType() + " " + getOperand(0).getName();
        }

        @Override
        public LoadInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (LoadInst) cloneMap.get(this);
            }
            LoadInst ret = new LoadInst(getType(), getOperand(0).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                                StoreInst Class
    //===----------------------------------------------------------------------===//

    /// 用于存储的指令
    public static class StoreInst extends Instruction {
        /**
         * alloca申请addr，store将val存到addr中
         *
         * @param val
         * @param addr
         */
        public StoreInst(Value val, Value addr) {
            super(Type.getVoidTy(), Ops.Store, 2);
            addOperand(val);
            addOperand(addr);
        }

        @Override
        public String toString() {
            return "store " + getOperand(0).getType().toString() + " " + getOperand(0).getName() + ", " +
                    getOperand(1).getType().toString() + " " + getOperand(1).getName();
        }

        @Override
        public StoreInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (StoreInst) cloneMap.get(this);
            }
            StoreInst ret = new StoreInst(getOperand(0).copy(cloneMap), getOperand(1).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                             GetElementPtrInst Class
    //===----------------------------------------------------------------------===//

    public static class GetElementPtrInst extends Instruction {
        private Type SourceElementType;//来源指针指向的类型
        private Type ResultElementType;//取址后得到的类型
        private Constant Init, ConstantValue;

        public GetElementPtrInst(Type type, String name, int numOperands) {
            super(type, Ops.GetElementPtr, name, numOperands);
        }

        /**
         * e.g. %elem_ptr = getelementptr [6 x i32], [6 x i32]* @a_gv, i32 0, i32 1
         *
         * @param PointeeType [6 x i32] 指向的类型
         * @param Ptr         [6 x i32]* @a_gv
         * @param IdxList     {i32 0, i32 1}
         * @param numOperands 3
         */
        public GetElementPtrInst(Type PointeeType, Value Ptr, ArrayList<Value> IdxList, int numOperands) {
            super(getGEPReturnType(PointeeType, Ptr, IdxList), Ops.GetElementPtr, numOperands);
            SourceElementType = PointeeType;
            ResultElementType = getIndexType(PointeeType, IdxList);
            addOperand(Ptr);
            addAllOperand(IdxList);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getName())
                    .append("= getelementptr ")
                    .append(((PointerType) getOperand(0).getType()).getElementType())
                    .append(",")
                    .append(getOperand(0).getType())
                    .append(" ")
                    .append(getOperand(0).getName())
                    .append(" ");
            for (var i = 1; i < getOperandList().size(); i++) {
                sb.append(", ").append(getOperand(i).getType()).append(" ").append(getOperand(i).getName());
            }
            StringBuilder comment=new StringBuilder();
            for(Value v:getArrayIdx()){
                comment.append("[");
                if(v instanceof AllocaInst) comment.append(v.getVarName());
                else comment.append(v.getName());
                comment.append("]");
            }
            setComment(comment.toString());
            return sb.toString();
        }

        public static Type getIndexType(Type Type, ArrayList<Value> IdxList) {
            if (IdxList.isEmpty()) {
                return Type;
            }
            for (Value V : IdxList.subList(1, IdxList.size())) {
                if (!(V.getType().isIntegerTy())) {
                    Type = null;
                }
                if (Type instanceof DerivedTypes.ArrayType) {
                    Type = ((DerivedTypes.ArrayType) Type).getKidType();
                }
                if (Type == null) {
                    return Type;
                }
            }
            return Type;
        }

        public static Type getGEPReturnType(Type ElTy, Value Ptr, ArrayList<Value> IdxList) {
            Type ty = getIndexType(ElTy, IdxList);
            assert ty != null;
            return PointerType.get(ty);
        }

        public static GetElementPtrInst create(Type PointeeType, Value Ptr, ArrayList<Value> IdxList) {
            int Values = 1 + IdxList.size();
            if (PointeeType == null) {
                PointeeType = ((DerivedTypes.PointerType) Ptr.getType()).getElementType();
            }
            return new GetElementPtrInst(PointeeType, Ptr, IdxList, Values);
        }

        public Type getSourceElementType() {
            return SourceElementType;
        }

        public void setSourceElementType(Type sourceElementType) {
            SourceElementType = sourceElementType;
        }

        public Type getResultElementType() {
            return ResultElementType;
        }

        public void setResultElementType(Type resultElementType) {
            ResultElementType = resultElementType;
        }

        public boolean allIndicesZero() {
            for (int i = 1; i < getNumOperands(); i++) {
                if (getOperand(i) instanceof Constants.ConstantInt) {
                    if (!((Constants.ConstantInt) getOperand(i)).isZero()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }

        /**
         * 根据gep的IdxList，计算gep的值
         * 返回值为null时，表示当前不可求出Constant
         */
        public Constant getConstantValue() {
            if (ConstantValue != null) return ConstantValue;
            Value source = getOperand(0);
            Constants.ConstantArray CA = null;
            if (getOperand(1) instanceof Constants.ConstantInt) {
                Constants.ConstantInt CI = (Constants.ConstantInt) getOperand(1);
                if (CI.getVal() != 0) {//%this = gep %prev 5
                    if (source instanceof GlobalVariable) { //%this = gep @gv 0, 0
                        CA = (Constants.ConstantArray) ((GlobalVariable) source).getOperand(0);
                    } else if (source instanceof GetElementPtrInst) { //%this = gep %prev 0, 1
                        CA = (Constants.ConstantArray) ((GetElementPtrInst) source).Init;
                    }
                    assert CA != null;
                    ConstantValue = (Constant) CA.getElement(CI.getVal());
                    return ConstantValue;
                }
            } else {
                return null;
            }
            if (source instanceof GlobalVariable) { //%this = gep @gv 0, 0
                CA = (Constants.ConstantArray) ((GlobalVariable) source).getOperand(0);
            } else if (source instanceof GetElementPtrInst) { //%this = gep %prev 0, 1
                CA = (Constants.ConstantArray) ((GetElementPtrInst) source).getConstantValue();
            }
            Init = CA;
            if (CA == null) return null;
            ConstantValue = CA;
            for (int i = 2; i < getNumOperands(); i++) {
                Value V = getOperand(i);
                if (V instanceof Constants.ConstantInt) {
                    Constants.ConstantInt CI = (Constants.ConstantInt) V;
                    int idx = CI.getVal();
                    ConstantValue = (Constant) ((Constants.ConstantArray) ConstantValue).getElement(idx);
                } else {
                    return null;
                }
            }
            return ConstantValue;
        }

        public ArrayList<Value> getArrayIdx(){
            if(AliasAnalysis.gepToArrayIdx.containsKey(this)){
                return AliasAnalysis.gepToArrayIdx.get(this);
            }
            ArrayList<Value> ret=new ArrayList<>();
            if(getOperand(0) instanceof GlobalVariable||getOperand(0) instanceof AllocaInst){//a
                ret.add(getOperand(0));
            }else if(getOperand(0) instanceof LoadInst){//数组参数
                LoadInst LI=(LoadInst)getOperand(0);
                AllocaInst AI=(AllocaInst)LI.getOperand(0);
                ret.add(AI);
                ret.add(Constants.ConstantInt.get(0));
            }else{
                GetElementPtrInst gep=(GetElementPtrInst)getOperand(0);
                ret=new ArrayList<>(gep.getArrayIdx());
            }
            if(ret.size()>1)
                ret.set(ret.size()-1, Folder.createAdd(ret.get(ret.size()-1),getOperand(1)));
            for(int i=2;i<getNumOperands();i++){
                ret.add(getOperand(i));
            }
            AliasAnalysis.gepToArrayIdx.put(this,ret);
            return ret;
        }

        @Override
        public GetElementPtrInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (GetElementPtrInst) cloneMap.get(this);
            }
            ArrayList<Value> Idx = new ArrayList<>();
            for (int i = 1; i < getNumOperands(); i++) {
                Idx.add(getOperand(i).copy(cloneMap));
            }
            GetElementPtrInst ret = new GetElementPtrInst(getSourceElementType(), getOperand(0).copy(cloneMap), Idx, getNumOperands());
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               ICmpInst Class
    //===----------------------------------------------------------------------===//

    public static class ICmpInst extends CmpInst {
        public ICmpInst(String name, Predicate pred, Value LHS, Value RHS) {
            super(Type.getInt1Ty(), Ops.ICmp, name, pred, LHS, RHS);
        }

        public ICmpInst(Predicate pred, Value LHS, Value RHS) {
            super(Type.getInt1Ty(), Ops.ICmp, pred, LHS, RHS);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getName()).append("= icmp ");
            switch (getPredicate()) {
                case ICMP_EQ -> sb.append("eq ");
                case ICMP_NE -> sb.append("ne ");
                case ICMP_SLT -> sb.append("slt ");
                case ICMP_SGT -> sb.append("sgt ");
                case ICMP_SLE -> sb.append("sle ");
                case ICMP_SGE -> sb.append("sge ");
            }
            sb.append(getOperand(0).getType()).append(" ");
            sb.append(getOperand(0).getName()).append(", ");
            sb.append(getOperand(1).getName());
            return sb.toString();
        }

        @Override
        public ICmpInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (ICmpInst) cloneMap.get(this);
            }
            ICmpInst ret = new ICmpInst(getPredicate(), getOperand(0).copy(cloneMap), getOperand(1).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               FCmpInst Class
    //===----------------------------------------------------------------------===//

    public static class FCmpInst extends CmpInst {
        public FCmpInst(String name, Predicate pred, Value LHS, Value RHS) {
            super(Type.getInt1Ty(), Ops.FCmp, name, pred, LHS, RHS);
        }

        public FCmpInst(Predicate pred, Value LHS, Value RHS) {
            super(Type.getInt1Ty(), Ops.FCmp, pred, LHS, RHS);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getName()).append("= fcmp ");
            switch (getPredicate()) {
                case FCMP_UEQ -> sb.append("ueq ");
                case FCMP_UNE -> sb.append("une ");
                case FCMP_ULT -> sb.append("ult ");
                case FCMP_UGT -> sb.append("ugt ");
                case FCMP_ULE -> sb.append("ule ");
                case FCMP_UGE -> sb.append("uge ");
            }
            sb.append(getOperand(0).getType()).append(" ");
            sb.append(getOperand(0).getName()).append(", ");
            sb.append(getOperand(1).getName());
            return sb.toString();
        }

        @Override
        public FCmpInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (FCmpInst) cloneMap.get(this);
            }
            FCmpInst ret = new FCmpInst(getPredicate(), getOperand(0).copy(cloneMap), getOperand(1).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               CallInst Class
    //===----------------------------------------------------------------------===//

    public static class CallInst extends Instruction {
        private FunctionType FTy;
        private ArrayList<Value> Attrs;

        public CallInst(FunctionType Ty, String name, Value Func, ArrayList<Value> Args) {
            super(Ty.getReturnType(), Ops.Call, name, Args.size() + 1);
            FTy = Ty;
            addOperand(Func);
            addAllOperand(Args);
        }

        public CallInst(FunctionType Ty, Value Func, ArrayList<Value> Args) {
            super(Ty.getReturnType(), Ops.Call, Args.size() + 1);
            FTy = Ty;
            addOperand(Func);
            addAllOperand(Args);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String funcName = getOperand(0).getName();
            if (funcName.equals("_sysy_stoptime")) {
                funcName = "stoptime";
            } else if (funcName.equals("_sysy_starttime")) {
                funcName = "starttime";
            }
            if (((FunctionType) getOperand(0).getType()).getReturnType().isVoidTy()) {
                sb.append("call ").append(this.getType()).append(" @").append(funcName);
            } else {
                sb.append(this.getName()).append(" = call ").append(this.getType()).append(" @")
                        .append(funcName);
            }

            sb.append("(");
            boolean hasArg = false;
            for (int i = 1; i < getOperandList().size(); i++) {
                hasArg = true;
                sb.append(getOperand(i).getType()).append(" ").append(getOperand(i).getName())
                        .append(",");
            }
            if (hasArg) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(")");
            return sb.toString();
        }

        public static CallInst create(Function Func, ArrayList<Value> Args) {
            return new CallInst(Func.getType(), Func, Args);
        }

        public Function getCalledFunction() {
            return (Function) getOperand(0);
        }

        public ArrayList<Value> getArgs() {
            return new ArrayList<>(getOperandList().subList(1, getNumOperands()));
        }

        @Override
        public CallInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (CallInst) cloneMap.get(this);
            }
            ArrayList<Value> args = new ArrayList<>();
            for (int i = 1; i < getNumOperands(); i++) {
                args.add(getOperand(i).copy(cloneMap));
            }
            CallInst ret = new CallInst(FTy, getOperand(0).copy(cloneMap), args);
            cloneMap.put(this, ret);
            return ret;
        }

        public boolean withoutGEP() {
            Function F = (Function) this.getOperand(0);
            if (F.hasSideEffect()) {
                return false;
            }
            for (Value val : this.getOperandList()) {
                if (val instanceof GetElementPtrInst ||
                        (val instanceof LoadInst && !val.getType().isInt32Ty() && !val.getType().isFloatTy())) {
                    return false;
                }
            }
            return true;
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 ZExtInst Class
    //===----------------------------------------------------------------------===//

    public static class ZExtInst extends CastInst {
        /**
         * @param type 指令类型
         * @param V    目标类型
         */
        public ZExtInst(Type type, Value V) {
            super(type, Ops.ZExt, V);
        }

        @Override
        public String toString() {
            return getName() + " = zext " + getOperand(0).getType() + " "
                    + getOperand(0).getName() + " to " + getType();
        }

        @Override
        public ZExtInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (ZExtInst) cloneMap.get(this);
            }
            ZExtInst ret = new ZExtInst(getType(), getOperand(0).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 SIToFPInst Class
    //===----------------------------------------------------------------------===//

    public static class SIToFPInst extends CastInst {
        /**
         * @param type 指令类型
         * @param V    目标类型
         */
        public SIToFPInst(Type type, Value V) {
            super(type, Ops.SIToFP, V);
        }

        @Override
        public String toString() {
            return getName() + " = sitofp " + getOperand(0).getType() + " "
                    + getOperand(0).getName() + " to " + getType();
        }

        @Override
        public SIToFPInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (SIToFPInst) cloneMap.get(this);
            }
            SIToFPInst ret = new SIToFPInst(getType(), getOperand(0).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 FPToSIInst Class
    //===----------------------------------------------------------------------===//

    public static class FPToSIInst extends CastInst {
        /**
         * @param type 指令类型
         * @param V    目标类型
         */
        public FPToSIInst(Type type, Value V) {
            super(type, Ops.FPToSI, V);
        }

        @Override
        public String toString() {
            return getName() + " = fptosi " + getOperand(0).getType() + " "
                    + getOperand(0).getName() + " to " + getType();
        }

        @Override
        public FPToSIInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (FPToSIInst) cloneMap.get(this);
            }
            FPToSIInst ret = new FPToSIInst(getType(), getOperand(0).copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               PHINode Class
    //===----------------------------------------------------------------------===//

    // Value存在OperandList中，BasicBlock存在blocks对应位置
    //
    public static class PHIInst extends Instruction {
        private final ArrayList<BasicBlock> blocks = new ArrayList<>();

        public PHIInst(Type ty, int block_number) {
            super(ty, Ops.PHI, 0);
        }

        public PHIInst(Type ty, int block_number, String Name, Instruction InsertBefore) {
            super(ty, Ops.PHI, 0, InsertBefore);
            setName(Name);
        }

        public PHIInst(Type ty, int block_number, String Name, BasicBlock InsertAtEnd) {
            super(ty, Ops.PHI, 0, InsertAtEnd);
            setName(Name);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getName()).append(" = phi ").append(getType()).append(" ");
            for (int i = 0; i < getNumOperands(); i++) {
                sb.append("[ ").append(getOperand(i).getName()).append(", %").append(getBlocks().get(i).getName()).append(" ] ");
                if (i != getNumOperands() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }

        public static PHIInst create(Type ty, int block_num) {
            return new PHIInst(ty, block_num);
        }

        public static PHIInst create(Type ty, int block_num, String Name, BasicBlock InsertAtEnd) {
            return new PHIInst(ty, block_num, Name, InsertAtEnd);
        }

        public static PHIInst create(Type ty, int block_num, String Name, Instruction InsertBefore) {
            return new PHIInst(ty, block_num, Name, InsertBefore);
        }

        public ArrayList<BasicBlock> getBlocks() {
            return blocks;
        }

        public void addIncomingValue(Value V) {
            addOperand(V);
        }

        public void addIncomingBlock(BasicBlock BB) {
            blocks.add(BB);
        }

        public void addIncoming(Value V, BasicBlock BB) {
            addIncomingValue(V);
            addIncomingBlock(BB);
            BB.getPHIs().add(this);
        }

        public void setIncomingBlock(int i, BasicBlock BB) {
            if (i > getNumOperands() || i < 0) return;
            blocks.set(i, BB);
            BB.getPHIs().add(this);
        }

        public void replaceIncomingBlock(BasicBlock OLD, BasicBlock BB) {
            int i = blocks.indexOf(OLD);
            if (i > getNumOperands() || i < 0) return;
            blocks.set(i, BB);
            BB.getPHIs().add(this);
        }

        public BasicBlock getIncomingBlock(int i) {
            if (i > getNumOperands()) return null;
            return blocks.get(i);
        }

        public BasicBlock getIncomingBlock(Use U) {
            if (this != U.getU()) return null;
            return blocks.get(getOperandList().indexOf(U.getVal()));
        }

        public Value getIncomingValue(int i) {
            if (i > getNumOperands()) return null;
            return getOperand(i);
        }

        public ArrayList<Value> getIncomingValues() {
            return getOperandList();
        }

        public Value getIncomingValueByBlock(BasicBlock BB) {
            int i = blocks.indexOf(BB);
            if (i > getNumOperands() || i < 0) return null;
            return getOperand(i);
        }

        /**
         * 设置来自BB的Value为V，若不存在BB，则添加
         */
        public void setOrAddIncomingValueByBlock(Value V, BasicBlock BB) {
            int i = blocks.indexOf(BB);
            if (i < 0) {
                addIncoming(V, BB);
            } else {
                setOperand(i, V);
            }
        }

        /**
         * 删除来自某基本块的Value
         *
         * @param BB         来源基本块
         * @param removeThis 在phi指令没有Value后，是否将其删除
         */
        public void removeIncomingValue(BasicBlock BB, boolean removeThis) {
            removeOperand(blocks.indexOf(BB));
            BB.getPHIs().remove(new Pair<>(this, blocks.indexOf(BB)));
            blocks.remove(BB);
            if (removeThis && getNumOperands() == 0) {
                replaceAllUsesWith(Constants.UndefValue.get(getType()));
                remove();
            }
        }

        /**
         * 若phi都返回同一个Value，则返回这个Value，否则返回null
         * 注意：可能返回phi本身 %1 = phi [%1, %br1] [%2, %br2]
         */
        public Value hasConstantValue() {
            Value ConstantValue = getIncomingValue(0);
            for (int i = 1, e = getNumOperands(); i != e; ++i)
                if (getIncomingValue(i) != ConstantValue && getIncomingValue(i) != this) {
                    if (ConstantValue != this)
                        return null;
                    ConstantValue = getIncomingValue(i);
                }
            if (ConstantValue == this)
                return Constants.UndefValue.get(getType());
            return ConstantValue;
        }

        /**
         * 重置PHI指令Value的来源
         *
         * @param BB    来源基本块（待合并）
         * @param Preds 来源基本块的前驱
         */
        public void redirectValuesFromPredecessors(BasicBlock BB, ArrayList<BasicBlock> Preds) {
            Value oldVal = getIncomingValueByBlock(BB);
            removeIncomingValue(BB, false);
            HashMap<BasicBlock, Value> incomingValues = new HashMap<>();
            for (int i = 0; i < getNumOperands(); i++) {
                //加入不是undef的value
                if (!Constants.UndefValue.isUndefValue(getIncomingValue(i))) {
                    incomingValues.put(getIncomingBlock(i), getIncomingValue(i));
                }
            }
            if (oldVal instanceof PHIInst && ((PHIInst) oldVal).getParent() == BB) {
                PHIInst oldValPI = (PHIInst) (oldVal);
                for (int i = 0; i < oldValPI.getNumOperands(); i++) {
                    //当前phi指令和oldValPI指令可能有相同的前驱，若来自oldValPI
                    //指令所在基本块的Value是undef，则考虑二者是否有共同前驱，有则
                    //用来自共同前驱的值替换；若不是undef，则没有共同前驱，或共同前驱
                    //的Value与OldValuePI的相同
                    BasicBlock PredBB = oldValPI.getIncomingBlock(i);
                    Value Selected = oldValPI.getIncomingValue(i);
                    if (Constants.UndefValue.isUndefValue(Selected)) {
                        //没有共同前驱
                        if (!incomingValues.containsKey(PredBB)) {
                            addIncoming(Selected, PredBB);
                        }
                    } else {
                        incomingValues.put(PredBB, Selected);
                        setOrAddIncomingValueByBlock(Selected, PredBB);
                    }
                }
            } else {
                for (BasicBlock PredBB : Preds) {
                    if (Constants.UndefValue.isUndefValue(oldVal)) {
                        //没有共同前驱
                        if (!incomingValues.containsKey(PredBB)) {
                            addIncoming(oldVal, PredBB);
                        }
                    } else {
                        incomingValues.put(PredBB, oldVal);
                        setOrAddIncomingValueByBlock(oldVal, PredBB);
                    }
                }
            }
        }

        /**
         * phi的复制比较复杂，可能造成循环，因此此处不进行incomingValues的copy
         */
        @Override
        public PHIInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (PHIInst) cloneMap.get(this);
            }
            PHIInst ret = new PHIInst(getType(), getNumOperands());
            ret.setName(getName() + cloneMap.hashCode());
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               ReturnInst Class
    //===----------------------------------------------------------------------===//
    //===---------------------------------------------------------------------------

    public static class ReturnInst extends Instruction {
        public ReturnInst(String name, Value retVal) {
            super(Type.getVoidTy(), Ops.Ret, name, retVal == null ? 0 : 1);
            if (retVal != null) {
                addOperand(retVal);
            }
        }

        public ReturnInst(Value retVal) {
            super(Type.getVoidTy(), Ops.Ret, retVal == null ? 0 : 1);
            if (retVal != null) {
                addOperand(retVal);
            }
        }

        public ReturnInst() {
            super(Type.getVoidTy(), Ops.Ret, 0);
        }

        public static ReturnInst create(Value retVal) {
            return new ReturnInst(retVal);
        }

        public static ReturnInst create() {
            return new ReturnInst();
        }

        public ArrayList<BasicBlock> getSuccessors() {
            return new ArrayList<>();
        }

        public int getSuccessorsNum() {
            return 0;
        }

        public BasicBlock getSuccessor(int idx) {
            return null;
        }

        public void setSuccessor(int idx, BasicBlock BB) {
            System.out.println("return指令没有后继基本块！");
        }

        @Override
        public String toString() {
            if (getOperandList().size() == 0) {
                return "ret void";
            }
            return "ret " + getOperand(0).getType() + " " + getOperand(0).getName();
        }

        @Override
        public ReturnInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (ReturnInst) cloneMap.get(this);
            }
            Value retVal = getNumOperands() == 1 ? getOperand(0).copy(cloneMap) : null;
            ReturnInst ret = new ReturnInst(retVal);
            cloneMap.put(this, ret);
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               BranchInst Class
    //===----------------------------------------------------------------------===//
    //===---------------------------------------------------------------------------

    public static class BranchInst extends Instruction {
        /// Ops list - Branches are strange.  The operands are ordered:
        ///  [Cond, FalseDest,] TrueDest.
        public BranchInst(BasicBlock IfTrue, String name) {
            super(Type.getVoidTy(), Ops.Br, name, 1);
        }

        public BranchInst(BasicBlock IfTrue) {
            super(Type.getVoidTy(), Ops.Br, 1);
            addOperand(IfTrue);
        }

        public BranchInst(BasicBlock IfTrue, BasicBlock InsertAtEnd) {
            super(Type.getVoidTy(), Ops.Br, 1, InsertAtEnd);
            addOperand(IfTrue);
        }

        public BranchInst(BasicBlock IfTrue, BasicBlock IfFalse, Value Cond) {
            super(Type.getVoidTy(), Ops.Br, 3);
            addOperand(Cond);
            addOperand(IfFalse);
            addOperand(IfTrue);
        }

        public void setCond(Value Cond) {
            setOperand(0, Cond);
        }

        public void setIfFalse(BasicBlock IfFalse) {
            setOperand(1, IfFalse);
        }

        public void setIfTrue(BasicBlock IfTrue) {
            setOperand(2, IfTrue);
        }

        public void setBr(BasicBlock BB) {
            setOperand(0, BB);
        }

        public BasicBlock getTrueBlock() {
            if (getNumOperands() == 3) {
                return (BasicBlock) getOperand(2);
            } else {
                return (BasicBlock) getOperand(0);
            }
        }

        public BasicBlock getFalseBlock() {
            if (getNumOperands() == 3) {
                return (BasicBlock) getOperand(1);
            } else {
                return (BasicBlock) getOperand(0);
            }
        }

        public Value getCond() {
            if (getNumOperands() == 3) {
                return getOperand(0);
            } else {
                return Constants.ConstantInt.const1_1();
            }
        }

        public ArrayList<BasicBlock> getSuccessors() {
            ArrayList<BasicBlock> ret = new ArrayList<>();
            if (getNumOperands() == 3) {
                ret.add(getTrueBlock());
                ret.add(getFalseBlock());
            } else {
                ret.add(getTrueBlock());
            }
            return ret;
        }

        public int getSuccessorsNum() {
            if (getNumOperands() == 3) {
                return 2;
            } else {
                return 1;
            }
        }

        public BasicBlock getSuccessor(int idx) {
            if (getNumOperands() == 3) {
                if (idx == 0) return getTrueBlock();
                else if (idx == 1) return getFalseBlock();
                else return null;
            } else {
                if (idx == 0) return getTrueBlock();
                else return null;
            }
        }

        public void setSuccessor(int idx, BasicBlock BB) {
            assert idx < getSuccessorsNum();
            if (getNumOperands() == 3) {
                idx++;
            }
            setOperand(idx, BB);
        }

        public boolean isConditional() {
            return getNumOperands() == 3;
        }

        @Override
        public String toString() {
            if (getOperandList().size() == 1) {
                return "br " + getOperand(0).getType() + " %" + getOperand(0).getName();
            }
            return "br " + getOperand(0).getType() + " " + getOperand(0).getName() + ", "
                    + getOperand(2).getType() + " %" + getOperand(2).getName() + ", "
                    + getOperand(1).getType() + " %" + getOperand(1).getName();
        }

        public static BranchInst create(BasicBlock IfTrue) {
            return new BranchInst(IfTrue);
        }

        public static BranchInst create(BasicBlock IfTrue, BasicBlock InsertAtEnd) {
            return new BranchInst(IfTrue, InsertAtEnd);
        }

        public static BranchInst create(BasicBlock IfTrue, BasicBlock IfFalse, Value Cond) {
            return new BranchInst(IfTrue, IfFalse, Cond);
        }

        @Override
        public BranchInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (BranchInst) cloneMap.get(this);
            }
            BranchInst ret = null;
            if (getNumOperands() == 1) {
                ret = new BranchInst((BasicBlock) getOperand(0).copy(cloneMap));
                cloneMap.put(this, ret);
            } else {
                ret = new BranchInst(getTrueBlock().copy(cloneMap), getFalseBlock().copy(cloneMap), getCond().copy(cloneMap));
                cloneMap.put(this, ret);
            }
            return ret;
        }
    }

    //===----------------------------------------------------------------------===//
    //                               SelectInst Class
    //===----------------------------------------------------------------------===//

    public static class SelectInst extends Instruction {
        public SelectInst(Value C, Value S1, Value S2) {
            super(S1.getType(), Ops.Select, 3);
            addOperand(C);
            addOperand(S1);
            addOperand(S2);
        }

        @Override
        public String toString() {
            return getName() + " = select " + getOperand(0).getType() + " " + getOperand(0).getName() + ", "
                    + getOperand(1).getType() + " " + getOperand(1).getName() + ", "
                    + getOperand(2).getType() + " " + getOperand(2).getName();
        }

        public static SelectInst create(Value C, Value S1, Value S2) {
            return new SelectInst(C, S1, S2);
        }

        public Value getCondition() {
            return getOperand(0);
        }

        public Value getTrueValue() {
            return getOperand(1);
        }

        public Value getFalseValue() {
            return getOperand(2);
        }

        public void setCondition(Value C) {
            setOperand(0, C);
        }

        public void setTrueValue(Value C) {
            setOperand(1, C);
        }

        public void setFalseValue(Value C) {
            setOperand(2, C);
        }

        @Override
        public SelectInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (SelectInst) cloneMap.get(this);
            }
            SelectInst ret = new SelectInst(getCondition().copy(cloneMap), getTrueValue().copy(cloneMap), getFalseValue().copy(cloneMap));
            cloneMap.put(this, ret);
            return ret;
        }
    }

    /**
     * 考虑在float的memset使用，现已废弃
     */
    public static class BitCastInst extends Instruction {
        private Type targetType;

        public BitCastInst(Value C, Type targetType) {
            super(targetType, Ops.BitCast, 1);
            this.targetType = targetType;
            addOperand(C);
        }

        @Override
        public String toString() {
            return getName() + " = bitcast " + getOperand(0).getType() + " " + getOperand(0).getName() + " to " + targetType;
        }

        public static BitCastInst create(Value C, Type target) {
            return new BitCastInst(C, target);
        }

        public Type getTargetType() {
            return targetType;
        }

        public void setTargetType(Type targetType) {
            this.targetType = targetType;
        }

        @Override
        public BitCastInst copy(CloneMap cloneMap) {
            if (cloneMap.get(this) != null) {
                return (BitCastInst) cloneMap.get(this);
            }
            BitCastInst ret = new BitCastInst(getOperand(0).copy(cloneMap), targetType);
            cloneMap.put(this, ret);
            return ret;
        }
    }
}
