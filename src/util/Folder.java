package util;

import ir.*;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;
import ir.instructions.Instructions.*;
import ir.Constants.*;
import ir.Instruction.Ops;

/**
 * 常量折叠
 */
public class Folder {
    public static Value simplifyInstruction(Instruction I){
        Value ret=null;
        switch (I.getOp()){
            case Add -> {
                ret=simplifyAdd(I);
            }
        }
        return ret;
    }

    public static Value simplifyAdd(Instruction I){
        Value ret=createAdd(I.getOperand(0),I.getOperand(1));
        if(ret!=null) return ret;
        return ret;
    }

    public static Constant createIcmp(CmpInst.Predicate P, Constant LHS, Constant RHS) {
        assert LHS instanceof ConstantInt && RHS instanceof ConstantInt;
        assert Instructions.ICmpInst.isIntPredicate(P);

        Type ResultType = Type.getInt1Ty();
        ConstantInt L = (ConstantInt) LHS;
        ConstantInt R = (ConstantInt) RHS;

        switch (P) {
            case ICMP_EQ:
                return ConstantInt.get(ResultType, L.getVal() == R.getVal() ? 1 : 0);
            case ICMP_NE:
                return ConstantInt.get(ResultType, L.getVal() != R.getVal() ? 1 : 0);
            case ICMP_SLT:
                return ConstantInt.get(ResultType, L.getVal() < R.getVal() ? 1 : 0);
            case ICMP_SGT:
                return ConstantInt.get(ResultType, L.getVal() > R.getVal() ? 1 : 0);
            case ICMP_SLE:
                return ConstantInt.get(ResultType, L.getVal() <= R.getVal() ? 1 : 0);
            case ICMP_SGE:
                return ConstantInt.get(ResultType, L.getVal() >= R.getVal() ? 1 : 0);
        }
        return null;
    }

    public static Constant createFcmp(CmpInst.Predicate P, Constant LHS, Constant RHS) {
        assert LHS instanceof ConstantFP && RHS instanceof ConstantFP;
        assert Instructions.ICmpInst.isIntPredicate(P);

        Type ResultType = Type.getInt1Ty();
        ConstantFP L = (ConstantFP) LHS;
        ConstantFP R = (ConstantFP) RHS;

        switch (P) {
            case FCMP_OEQ, FCMP_UEQ:
                return ConstantInt.get(ResultType, L.getVal() == R.getVal() ? 1 : 0);
            case FCMP_ONE, FCMP_UNE:
                return ConstantInt.get(ResultType, L.getVal() != R.getVal() ? 1 : 0);
            case FCMP_OLT, FCMP_ULT:
                return ConstantInt.get(ResultType, L.getVal() < R.getVal() ? 1 : 0);
            case FCMP_OGT, FCMP_UGT:
                return ConstantInt.get(ResultType, L.getVal() > R.getVal() ? 1 : 0);
            case FCMP_OLE, FCMP_ULE:
                return ConstantInt.get(ResultType, L.getVal() <= R.getVal() ? 1 : 0);
            case FCMP_OGE, FCMP_UGE:
                return ConstantInt.get(ResultType, L.getVal() >= R.getVal() ? 1 : 0);
        }
        return null;
    }

    public static Constant createCast(Instruction.Ops Op, Constant C, Type Ty) {
        switch (Op) {
            case ZExt:
                if (C instanceof ConstantInt) {
                    return ConstantInt.get(Ty, ((ConstantInt) C).getVal());
                }
                return null;
            case SIToFP:
                if (C instanceof ConstantInt) {
                    float val = (float) ((ConstantInt) C).getVal();
                    return ConstantFP.get(val);
                }
                return null;
            case FPToSI:
                if (C instanceof ConstantFP) {
                    int val = (int) ((ConstantFP) C).getVal();
                    return ConstantInt.get(val);
                }
                return null;
        }
        return null;
    }

    /**
     * 需要保证LHS和RHS至少有一个为Constant，返回null表示无法折叠
     */
    public static Value createBinOp(Instruction.Ops Opc, Value LHS, Value RHS) {
        if (RHS instanceof ConstantInt) {
            ConstantInt CI2 = (ConstantInt) RHS;
            switch (Opc) {
                case Sub:
                case Add:
                    // X - 0 == X
                    // X + 0 == X
                    if (CI2.getVal() == 0) {
                        return LHS;
                    }
                    break;
                case Mul:
                    // X * 0 == 0
                    // X * 1 == X
                    if (CI2.getVal() == 0) {
                        return RHS;
                    }
                    if (CI2.getVal() == 1) {
                        return LHS;
                    }
                    break;
                case SDiv:
                    // X / 1 == X
                    if (CI2.getVal() == 1) {
                        return LHS;
                    }
                    break;
                case SRem:
                    // X % 1 == 0
                    if (Math.abs(CI2.getVal()) == 1) {
                        return ConstantInt.const_0();
                    }
                    break;
                case And:
                    // X & 0 == 0
                    // X & -1 == X
                    if (CI2.getVal() == 0) return RHS;
                    if (CI2.getVal() == -1)
                        return LHS;
                    break;
                case Or:
                    // X | 0 == X
                    // X | -1 == -1
                    if (CI2.getVal() == 0) return LHS;
                    if (CI2.getVal() == -1)
                        return RHS;
                    break;
                case Xor:
                    // X ^ 0 == X
                    if (CI2.getVal() == 0) return LHS;
                    break;
            }
        } else if (LHS instanceof ConstantInt) {
            // If C1 is a ConstantInt and C2 is not, swap the operands.
            if (Instruction.isCommutative(Opc)) {
                return createBinOp(Opc, RHS, LHS);
            }
        }else if (RHS instanceof ConstantFP) {
            ConstantFP CI2 = (ConstantFP) RHS;
            switch (Opc) {
                case FSub:
                case FAdd:
                    // X - 0 == X
                    // X + 0 == X
                    if (CI2.getVal() == 0) {
                        return LHS;
                    }
                    break;
                case FMul:
                    // X * 0 == 0
                    // X * 1 == X
                    if (CI2.getVal() == 0) {
                        return RHS;
                    }
                    if (CI2.getVal() == 1) {
                        return LHS;
                    }
                    break;
                case FDiv:
                    // X / 1 == X
                    if (CI2.getVal() == 1) {
                        return LHS;
                    }
                    break;
            }
        } else if (LHS instanceof ConstantFP) {
            // If C1 is a ConstantFP and C2 is not, swap the operands.
            if (Instruction.isCommutative(Opc)) {
                return createBinOp(Opc, RHS, LHS);
            }
        }

        if (LHS instanceof ConstantInt) {
            ConstantInt CI1 = (ConstantInt) LHS;
            if (RHS instanceof ConstantInt) {
                ConstantInt CI2 = (ConstantInt) RHS;
                int C1V = CI1.getVal(), C2V = CI2.getVal();
                switch (Opc) {
                    default:
                        break;
                    case Sub:
                        return ConstantInt.get(C1V - C2V);
                    case Add:
                        return ConstantInt.get(C1V + C2V);
                    case Mul:
                        return ConstantInt.get(C1V * C2V);
                    case SDiv:
                        assert C2V != 0;
                        return ConstantInt.get(C1V / C2V);
                    case SRem:
                        assert C2V != 0;
                        return ConstantInt.get(C1V % C2V);
                    case And:
                        return ConstantInt.get(C1V & C2V);
                    case Or:
                        return ConstantInt.get(C1V | C2V);
                    case Xor:
                        return ConstantInt.get(C1V ^ C2V);
                }
            }
            switch (Opc) {
                case SDiv:
                case SRem:
                    if (LHS.equals(ConstantInt.const_0())) {
                        return LHS;
                    }
                    break;
                default:
                    break;
            }
        } else if (LHS instanceof ConstantFP) {
            ConstantFP CFP1 = (ConstantFP) LHS;
            if (RHS instanceof ConstantFP) {
                ConstantFP CFP2 = (ConstantFP) RHS;
                float C1V = CFP1.getVal(), C2V = CFP2.getVal();
                switch (Opc) {
                    default:
                        break;
                    case FSub:
                        return ConstantFP.get(C1V - C2V);
                    case FAdd:
                        return ConstantFP.get(C1V + C2V);
                    case FMul:
                        return ConstantFP.get(C1V * C2V);
                    case FDiv:
                        assert C2V != 0;
                        return ConstantFP.get(C1V / C2V);
                    case FRem:
                        assert C2V != 0;
                        return ConstantFP.get(C1V % C2V);
                }
            }
        }
        return null;
    }

    public static Value createAdd(Value LHS, Value RHS) {
        return createBinOp(Ops.Add, LHS, RHS);
    }

    public static Value createFAdd(Value LHS, Value RHS) {
        return createBinOp(Ops.FAdd, LHS, RHS);
    }

    public static Value createSub(Value LHS, Value RHS) {
        return createBinOp(Ops.Sub, LHS, RHS);
    }

    public static Value createFSub(Value LHS, Value RHS) {
        return createBinOp(Ops.FSub, LHS, RHS);
    }

    public static Value createMul(Value LHS, Value RHS) {
        return createBinOp(Ops.Mul, LHS, RHS);
    }

    public static Value createFMul(Value LHS, Value RHS) {
        return createBinOp(Ops.FMul, LHS, RHS);
    }

    public static Value createSDiv(Value LHS, Value RHS) {
        return createBinOp(Ops.SDiv, LHS, RHS);
    }

    public static Value createFDiv(Value LHS, Value RHS) {
        return createBinOp(Ops.FDiv, LHS, RHS);
    }

    public static Value createAnd(Value LHS, Value RHS) {
        return createBinOp(Ops.And, LHS, RHS);
    }

    public static Value createOr(Value LHS, Value RHS) {
        return createBinOp(Ops.Or, LHS, RHS);
    }

    public static Value createNot(Value C) {
        return createBinOp(Ops.Xor, C, ConstantInt.const1_1());
    }

    public static Constant createSelect(Constant C, Constant True, Constant False) {
        if (C.isNullValue()) return False;
        if (C instanceof ConstantInt && ((ConstantInt) C).getVal() != 0) return True;
        if (True == False) return True;
        return null;
    }

    /**
     * 化简终结指令（br i1 %1, label %2, label %2 或 br i1 1, label %2, label %3）-> (br label %2)
     */
    public static boolean constantFoldTerminator(BasicBlock BB) {
        Instruction T = BB.getTerminator();
        MyIRBuilder builder = MyIRBuilder.getInstance();
        builder.setInsertPoint(T);
        if (T instanceof BranchInst) {
            //br label不用优化
            if (T.getNumOperands() == 1) return false;
            BranchInst BI = (BranchInst) T;
            BasicBlock TrueBlock = (BI).getTrueBlock();
            BasicBlock FalseBlock = BI.getFalseBlock();
            if (TrueBlock == FalseBlock) {
                TrueBlock.removePredecessor(BB);
                BranchInst newBI = (BranchInst) builder.createBr(TrueBlock);
                BI.remove();
                //TODO:删除BI的Cond
                return true;
            }

            if (BI.getCond() instanceof ConstantInt) {
                ConstantInt cond = (ConstantInt) BI.getCond();
                BasicBlock dest = cond.getVal() == 1 ? TrueBlock : FalseBlock;
                BasicBlock oldDest = cond.getVal() == 0 ? TrueBlock : FalseBlock;

                oldDest.removePredecessor(BB);
                BranchInst newBI = (BranchInst) builder.createBr(dest);
                BI.remove();
                return true;
            }
            return false;
        }
        return false;
    }
}
