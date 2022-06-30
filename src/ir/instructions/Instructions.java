package ir.instructions;

import ir.Value;
import ir.*;
import ir.DerivedTypes.*;

import java.util.ArrayList;

public abstract class Instructions {
    //===----------------------------------------------------------------------===//
    //                                AllocaInst Class
    //===----------------------------------------------------------------------===//

    /// 在堆栈上分配局部变量的指令
    public static class AllocaInst extends UnaryInstruction {
        private Type AllocatedType;
        private boolean undef = true;

        /**
         * 申请addr，储存type类型的val
         *
         * @param type 储存Value的类型
         */
        public AllocaInst(Type type) {
            super(DerivedTypes.PointerType.get(type), Ops.Alloca, null);
            AllocatedType = type;
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
            super(type, Ops.Fence, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                             GetElementPtrInst Class
    //===----------------------------------------------------------------------===//

    public static class GetElementPtrInst extends Instruction {
        private Type SourceElementType;
        private Type ResultElementType;

        public GetElementPtrInst(Type type, String name, int numOperands) {
            super(type, Ops.GetElementPtr, name, numOperands);
        }

        /**
         * e.g. %elem_ptr = getelementptr [6 x i8], [6 x i8]* @a_gv, i32 0, i32 1
         *
         * @param PointeeType [6 x i8] 指向的类型
         * @param Ptr         [6 x i8]* @a_gv
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
            return sb.toString();
        }

        public static Type getIndexType(Type Type, ArrayList<Value> IdxList) {
            if (IdxList.isEmpty()) {
                return Type;
            }
            for (Value V : IdxList.subList(1,IdxList.size())) {
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
    }

    //===----------------------------------------------------------------------===//
    //                               ICmpInst Class
    //===----------------------------------------------------------------------===//

    /// This instruction compares its operands according to the predicate given
    /// to the constructor. It only operates on integers or pointers. The operands
    /// must be identical types.
    /// Represent an integer comparison operator.
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
    }

    //===----------------------------------------------------------------------===//
    //                               FCmpInst Class
    //===----------------------------------------------------------------------===//
    /// This instruction compares its operands according to the predicate given
    /// to the constructor. It only operates on floating point values or packed
    /// vectors of floating point values. The operands must be identical types.
    /// Represents a floating point comparison operator.
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
            if (((FunctionType) getOperand(0).getType()).getReturnType().isVoidTy()) {
                sb.append("call ").append(this.getType()).append(" @").append(getOperand(0).getName());
            } else {
                sb.append(this.getName()).append(" = call ").append(this.getType()).append(" @")
                        .append(getOperand(0).getName());
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
            super(type, Ops.CallBr, name, numOperands);
        }

        public CallBrInst(Type type, int numOperands) {
            super(type, Ops.CallBr, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 ZExtInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents zero extension of integer types.
    public static class ZExtInst extends CastInst {
        /**
         * @param type 指令类型
         * @param V 目标类型
         */
        public ZExtInst(Type type, Value V) {
            super(type, Ops.ZExt, V);
        }

        @Override
        public String toString() {
            return getName() + " = zext " + getOperand(0).getType() + " "
                    + getOperand(0).getName() + " to " + getType();
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 SIToFPInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents a cast from signed integer to floating point.
    public static class SIToFPInst extends CastInst {
        /**
         * @param type 指令类型
         * @param V 目标类型
         */
        public SIToFPInst(Type type, Value V) {
            super(type, Ops.SIToFP, V);
        }

        @Override
        public String toString() {
            return getName() + " = sitofp " + getOperand(0).getType() + " "
                    + getOperand(0).getName() + " to " + getType();
        }
    }

    //===----------------------------------------------------------------------===//
    //                                 FPToSIInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents a cast from floating point to signed integer.
    public static class FPToSIInst extends CastInst {
        /**
         * @param type 指令类型
         * @param V 目标类型
         */
        public FPToSIInst(Type type, Value V) {
            super(type, Ops.FPToSI, V);
        }

        @Override
        public String toString() {
            return getName() + " = fptosi " + getOperand(0).getType() + " "
                    + getOperand(0).getName() + " to " + getType();
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

        @Override
        public String toString() {
            if(getOperandList().size()==0){
                return "ret void";
            }
            return "ret " + getOperand(0).getType() + " " + getOperand(0).getName();
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
            super(Type.getVoidTy(), Ops.Br, name, 1);
        }

        public BranchInst(BasicBlock IfTrue) {
            super(Type.getVoidTy(), Ops.Br, 1);
            addOperand(IfTrue);
        }

        public BranchInst(BasicBlock IfTrue, BasicBlock IfFalse, Value Cond) {
            super(Type.getVoidTy(), Ops.Br, 3);
            addOperand(Cond);
            addOperand(IfFalse);
            addOperand(IfTrue);
        }

        public void setCond(Value Cond){
            setOperand(0,Cond);
        }

        public void setIfFalse(BasicBlock IfFalse){
            setOperand(1,IfFalse);
        }

        public void setIfTrue(BasicBlock IfTrue){
            setOperand(2,IfTrue);
        }

        public void setBr(BasicBlock BB){
            setOperand(0,BB);
        }

        @Override
        public String toString() {
            if(getOperandList().size()==1){
                return "br "+getOperand(0).getType()+" %"+getOperand(0).getName();
            }
            return "br "+getOperand(0).getType()+" "+getOperand(0).getName()+", "
                    +getOperand(2).getType()+" %"+getOperand(2).getName()+", "
                    +getOperand(1).getType()+" %"+getOperand(1).getName();
        }

        public static BranchInst create(BasicBlock IfTrue) {
            return new BranchInst(IfTrue);
        }

        public static BranchInst create(BasicBlock IfTrue, BasicBlock IfFalse, Value Cond) {
            return new BranchInst(IfTrue, IfFalse, Cond);
        }
    }

    //===----------------------------------------------------------------------===//
    //                               SelectInst Class
    //===----------------------------------------------------------------------===//
    /// This class represents the LLVM 'select' instruction.
    ///
    public static class SelectInst extends Instruction {
        public SelectInst(Value C, Value S1, Value S2) {
            super(S1.getType(), Ops.Select, 3);
            addOperand(C);
            addOperand(S1);
            addOperand(S2);
        }

        @Override
        public String toString() {
            return getName() + " = select "+getOperand(0).getType()+" "+getOperand(0).getName()+", "
                    +getOperand(1).getType()+" "+getOperand(1).getName()+", "
                    +getOperand(2).getType()+" "+getOperand(2).getName();
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
    }
}
