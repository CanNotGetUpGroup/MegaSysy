package ir.instructions;

import ir.*;
import ir.DerivedTypes.*;

import java.util.ArrayList;

public abstract class Instructions {
    //===----------------------------------------------------------------------===//
    //                                AllocaInst Class
    //===----------------------------------------------------------------------===//

    /// an instruction to allocate memory on the stack
    public static class AllocaInst extends UnaryInstruction {
        private Type AllocatedType;
        private boolean undef=true;

        /**
         * 申请addr，储存type类型的val
         * @param type 储存Value的类型
         */
        public AllocaInst(Type type) {
            super(DerivedTypes.PointerType.get(type),Ops.Alloca, null);
            AllocatedType=type;
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
    }

    //===----------------------------------------------------------------------===//
    //                                LoadInst Class
    //===----------------------------------------------------------------------===//

    /// An instruction for reading from memory. This uses the SubclassData field in
    /// Value to store whether or not the load is volatile.
    public static class LoadInst extends UnaryInstruction{
        /**
         * load从addr中读取储存的val
         * @param type 取得Value的类型
         * @param addr 对应的alloca指令
         */
        public LoadInst(Type type,Value addr) {
            super(type,Ops.Load,addr);
            assert type.equals(((DerivedTypes.PointerType)addr.getType()).getElementType());
        }
    }

    //===----------------------------------------------------------------------===//
    //                                StoreInst Class
    //===----------------------------------------------------------------------===//

    /// An instruction for storing to memory.
    public static class StoreInst extends Instruction {
        /**
         * alloca申请addr，store将val存到addr中
         * @param val
         * @param addr
         */
        public StoreInst(Value val, Value addr) {
            super(Type.getVoidTy(), Ops.Store, 2);
            addOperand(val);
            addOperand(addr);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                FenceInst Class
    //===----------------------------------------------------------------------===//

    /// An instruction for ordering other memory operations.
    public static class FenceInst extends Instruction {
        public FenceInst(Type type, String name, int numOperands) {
            super(type, Ops.Fence, name, numOperands);
        }

        public FenceInst(Type type, int numOperands) {
            super(type,Ops.Fence, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                             GetElementPtrInst Class
    //===----------------------------------------------------------------------===//

    // checkGEPType - Simple wrapper function to give a better assertion failure
    // message on bad indexes for a gep instruction.
    //
    public static class GetElementPtrInst extends Instruction {
        private Type SourceElementType;
        private Type ResultElementType;

        public GetElementPtrInst(Type type, String name, int numOperands) {
            super(type, Ops.GetElementPtr,name, numOperands);
        }

        /**
         * e.g. %elem_ptr = getelementptr [6 x i8], [6 x i8]* @a_gv, i32 0, i32 1
         * @param PointeeType [6 x i8] 指向的类型
         * @param Ptr [6 x i8]* @a_gv
         * @param IdxList {i32 0, i32 1}
         * @param numOperands 3
         */
        public GetElementPtrInst(Type PointeeType,Value Ptr,ArrayList<Value> IdxList, int numOperands) {
            super(getGEPReturnType(PointeeType, Ptr, IdxList),Ops.GetElementPtr, numOperands);
            SourceElementType = PointeeType;
            ResultElementType = getIndexType(PointeeType, IdxList);
            addOperand(Ptr);
            addAllOperand(IdxList);
        }

        public static Type getIndexType(Type Type,ArrayList<Value> IdxList){
            if(IdxList.isEmpty()){
                return Type;
            }
            for(Value V:IdxList){
                if(!(V.getType().isIntegerTy())){
                    Type = null;
                }
                if(Type instanceof DerivedTypes.ArrayType){
                    Type = ((DerivedTypes.ArrayType)Type).getKidType();
                }
                if(Type==null){
                    return Type;
                }
            }
            return Type;
        }

        public static Type getGEPReturnType(Type ElTy,Value Ptr,ArrayList<Value> IdxList){
            Type ty=getIndexType(ElTy,IdxList);
            assert ty!=null;
            Type PtrTy= DerivedTypes.PointerType.get(ty);
            //TODO:Vector
            return PtrTy;
        }

        public static GetElementPtrInst create(Type PointeeType, Value Ptr, ArrayList<Value> IdxList){
            int Values = 1+IdxList.size();
            if(PointeeType==null){
                PointeeType=((DerivedTypes.PointerType)Ptr.getType()).getElementType();
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
    }

    //===----------------------------------------------------------------------===//
    //                               ICmpInst Class
    //===----------------------------------------------------------------------===//

    /// This instruction compares its operands according to the predicate given
    /// to the constructor. It only operates on integers or pointers. The operands
    /// must be identical types.
    /// Represent an integer comparison operator.
    public static class ICmpInst extends CmpInst {
        public ICmpInst(String name,Predicate pred, Value LHS,Value RHS) {
            super(LHS.getType(),Ops.ICmp,name,pred,LHS,RHS);
        }

        public ICmpInst(Predicate pred, Value LHS,Value RHS) {
            super(LHS.getType(),Ops.ICmp, pred,LHS,RHS);
        }
    }

    //===----------------------------------------------------------------------===//
    //                               FCmpInst Class
    //===----------------------------------------------------------------------===//
    /// This instruction compares its operands according to the predicate given
    /// to the constructor. It only operates on floating point values or packed
    /// vectors of floating point values. The operands must be identical types.
    /// Represents a floating point comparison operator.
    public static class FCmpInst extends CmpInst {
        public FCmpInst(String name,Predicate pred, Value LHS,Value RHS) {
            super(LHS.getType(),Ops.FCmp,name,pred,LHS,RHS);
        }

        public FCmpInst(Predicate pred, Value LHS,Value RHS) {
            super(LHS.getType(),Ops.FCmp,pred,LHS,RHS);
        }
    }

    //===----------------------------------------------------------------------===//
    //                               CallInst Class
    //===----------------------------------------------------------------------===//
    //===----------------------------------------------------------------------===//
    /// This class represents a function call, abstracting a target
    /// machine's calling convention.  This class uses low bit of the SubClassData
    /// field to indicate whether or not this is a tail call.  The rest of the bits
    /// hold the calling convention of the call.
    ///
    public static class CallInst extends Instruction {
        private FunctionType FTy;
        private ArrayList<Value> Attrs;
        public CallInst(FunctionType Ty, String name,Value Func,ArrayList<Value> Args) {
            super(Ty.getReturnType(),Ops.Call,name,Args.size()+1);
            FTy=Ty;
            addOperand(Func);
            addAllOperand(Args);
        }

        public CallInst(FunctionType Ty,Value Func,ArrayList<Value> Args) {
            super(Ty.getReturnType(),Ops.Call,Args.size()+1);
            FTy=Ty;
            addOperand(Func);
            addAllOperand(Args);
        }

        public static CallInst create(Function Func,ArrayList<Value> Args){
            return new CallInst((FunctionType) Func.getType(),Func,Args);
        }
    }

    //===----------------------------------------------------------------------===//
    //                              CallBrInst Class
    //===----------------------------------------------------------------------===//
    /// CallBr instruction, tracking function calls that may not return control but
    /// instead transfer it to a third location. The SubclassData field is used to
    /// hold the calling convention of the call.
    ///
    public static class CallBrInst extends Instruction {
        public CallBrInst(Type type, String name, int numOperands) {
            super(type,Ops.CallBr, name, numOperands);
        }

        public CallBrInst(Type type, int numOperands) {
            super(type,Ops.CallBr, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 ZExtInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents zero extension of integer types.
    public static class ZExtInst extends CastInst {
        public ZExtInst(Type type, Value V) {
            super(type,Ops.ZExt, V);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 SIToFPInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents a cast from signed integer to floating point.
    public static class SIToFPInst extends CastInst {
        public SIToFPInst(Type type, Value V) {
            super(type,Ops.SIToFP, V);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 FPToSIInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents a cast from floating point to signed integer.
    public static class FPToSIInst extends CastInst {
        public FPToSIInst(Type type, Value V) {
            super(type,Ops.FPToSI, V);
        }
    }

    //===----------------------------------------------------------------------===//
    //                               ReturnInst Class
    //===----------------------------------------------------------------------===//
    //===---------------------------------------------------------------------------
    /// Return a value (possibly void), from a function.  Execution
    /// does not continue in this function any longer.
    ///
    public static class ReturnInst extends Instruction {
        public ReturnInst(String name, Value retVal) {
            super(Type.getVoidTy(),Ops.Ret,name, retVal==null?0:1);
            if(retVal!=null){
                addOperand(retVal);
            }
        }

        public ReturnInst(Value retVal) {
            super(Type.getVoidTy(),Ops.Ret, retVal==null?0:1);
            if(retVal!=null){
                addOperand(retVal);
            }
        }

        public ReturnInst() {
            super(Type.getVoidTy(),Ops.Ret,0);
        }

        public static ReturnInst create(Value retVal){
            return new ReturnInst(retVal);
        }

        public static ReturnInst create(){
            return new ReturnInst();
        }
    }

    //===----------------------------------------------------------------------===//
    //                               BranchInst Class
    //===----------------------------------------------------------------------===//
    //===---------------------------------------------------------------------------
    /// Conditional or Unconditional Branch instruction.
    ///
    public static class BranchInst extends Instruction {
        /// Ops list - Branches are strange.  The operands are ordered:
        ///  [Cond, FalseDest,] TrueDest.
        public BranchInst(BasicBlock IfTrue, String name) {
            super(Type.getVoidTy(),Ops.Br, name, 1);
        }

        public BranchInst(BasicBlock IfTrue) {
            super(Type.getVoidTy(),Ops.Br, 1);
            addOperand(IfTrue);
        }

        public BranchInst(BasicBlock IfTrue,BasicBlock IfFalse,Value Cond) {
            super(Type.getVoidTy(),Ops.Br, 3);
            addOperand(Cond);
            addOperand(IfFalse);
            addOperand(IfTrue);
        }

        public static BranchInst create(BasicBlock IfTrue){
            return new BranchInst(IfTrue);
        }

        public static BranchInst create(BasicBlock IfTrue,BasicBlock IfFalse,Value Cond){
            return new BranchInst(IfTrue,IfFalse,Cond);
        }
    }

    //===----------------------------------------------------------------------===//
    //                               SelectInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents the LLVM 'select' instruction.
    ///
    public static class SelectInst extends Instruction {
        public SelectInst(Value C, Value S1, Value S2) {
            super(S1.getType(),Ops.Select, 3);
            addOperand(C);
            addOperand(S1);
            addOperand(S2);
        }

        public static SelectInst create(Value C, Value S1, Value S2){
            return new SelectInst(C,S1,S2);
        }

        public Value getCondition() { return getOperand(0); }
        public Value getTrueValue() { return getOperand(1); }
        public Value getFalseValue() { return getOperand(2); }

        public void setCondition(Value C) { setOperand(0,C); }
        public void setTrueValue(Value C) { setOperand(1,C); }
        public void setFalseValue(Value C) { setOperand(2,C); }
    }
}
