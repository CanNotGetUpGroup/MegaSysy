package util;

import ir.*;
import ir.instructions.BinaryInstruction;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;
import ir.instructions.Instructions.*;
import ir.Constants.*;
import ir.Instruction.Ops;
import org.stringtemplate.v4.misc.STModelAdaptor;
import pass.PassManager;
import util.Match.*;

/**
 * 指令化简和常量折叠
 */
public class Folder {
    //指令化简
    public static Instruction InsertBefore;

    public static Value simplifyInstruction(Instruction I) {
        InsertBefore = I;
        Value ret = null;
        int recurseTimes = 3;
        if (Instruction.isBinary(I.getOp())) {
            ret = simplifyBin(I.getOp(), I.getOperand(0), I.getOperand(1), recurseTimes);
        }
        switch (I.getOp()) {
            case PHI -> {
                ret = simplifyPhi((PHIInst) I);
            }
            case ICmp -> {
                ret = simplifyICmp(((CmpInst) I).getPredicate(), I.getOperand(0), I.getOperand(1));
            }
            case FCmp -> {
                ret = simplifyFCmp(((CmpInst) I).getPredicate(), I.getOperand(0), I.getOperand(1));
            }
            case ZExt -> {
                ret = simplifyCast(I.getOp(), I.getOperand(0), I.getType());
            }
        }
        return ret;
    }

    public static Value simplifyAdd(Value X, Value Y, int recurse) {
        Value ret = createAdd(X, Y);
        if (ret != null) return ret;
        if (X instanceof Constant) {
            Value tmp = X;
            X = Y;
            Y = tmp;
        }
        Match MX = Match.createMatch(X), MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantInt.get(0));
        // X = sub (0, Y) || Y = sub (0, X)
        if (Match.compare(X, new MatchBin(M0, MY, Ops.Sub))
                || Match.compare(Y, new MatchBin(M0, MX, Ops.Sub))) {
            return ConstantInt.get(0);
        }
        // X = sub (A, B), Y = sub (B, A)
        MatchUndef MA = new MatchUndef(), MB = new MatchUndef();
        if (Match.compare(X, new MatchBin(MA, MB, Ops.Sub))
                && Match.compare(Y, new MatchBin(Match.createMatch(MB.V), Match.createMatch(MA.V), Ops.Sub))) {
            return ConstantInt.get(0);
        }

        // X + (Y - X) -> Y
        // (Y - X) + X -> Y
        MatchUndef MU = new MatchUndef();
        if (Match.compare(Y, new MatchBin(MU, MX, Ops.Sub))
                || Match.compare(X, new MatchBin(MU, MY, Ops.Sub))) {
            return MU.V;
        }

        // (Y + Y) + Y -> 3*Y
        if (Match.compare(X, new MatchBin(MY, MY, Ops.Add))) {
            return BinaryInstruction.create(Ops.Mul, Y, ConstantInt.get(3), InsertBefore);
        }
        // X + (X + X) -> 3*X
        if (Match.compare(Y, new MatchBin(MX, MX, Ops.Add))) {
            return BinaryInstruction.create(Ops.Mul, X, ConstantInt.get(3), InsertBefore);
        }
        // (Y * A) + Y -> (A+1)*Y || (A * Y) + Y -> Y * (A+1)
        MU=new MatchUndef();
        if (Match.compare(X, new MatchBin(MY, MU, Ops.Mul))&&(MU.V instanceof ConstantInt)) {
            int c=((ConstantInt)MU.V).getVal();
            return BinaryInstruction.create(Ops.Mul, Y, ConstantInt.get(c+1), InsertBefore);
        }
        // X + (X * A) -> (A+1)*X || X + (X * A) -> X * (A+1)
        MU=new MatchUndef();
        if (Match.compare(Y, new MatchBin(MX, MU, Ops.Mul))&&(MU.V instanceof ConstantInt)) {
            int c=((ConstantInt)MU.V).getVal();
            return BinaryInstruction.create(Ops.Mul, X, ConstantInt.get(c+1), InsertBefore);
        }
        ret = simplifyAssociative(Ops.Add, X, Y, recurse);
        return ret;
    }

    public static Value simplifyFAdd(Value X, Value Y, int recurse) {
        Value ret = createFAdd(X, Y);
        if (ret != null) return ret;
        if (X instanceof Constant) {
            Value tmp = X;
            X = Y;
            Y = tmp;
        }
        Match MX = Match.createMatch(X), MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantFP.get(0));
        // X = sub (0, Y) || Y = sub (0, X)
        if (Match.compare(X, new MatchBin(M0, MY, Ops.FSub))
                || Match.compare(Y, new MatchBin(M0, MX, Ops.FSub))) {
            return ConstantFP.get(0);
        }
        // X = sub (A, B), Y = sub (B, A)
        MatchUndef MA = new MatchUndef(), MB = new MatchUndef();
        if (Match.compare(X, new MatchBin(MA, MB, Ops.FSub))
                && Match.compare(Y, new MatchBin(Match.createMatch(MB.V), Match.createMatch(MA.V), Ops.Sub))) {
            return ConstantFP.get(0);
        }

        // X + (Y - X) -> Y
        // (Y - X) + X -> Y
        MatchUndef MU = new MatchUndef();
        if (Match.compare(Y, new MatchBin(MU, MX, Ops.FSub))
                || Match.compare(X, new MatchBin(MU, MY, Ops.FSub))) {
            return MU.V;
        }

        // (Y + Y) + Y -> 3*Y
        if (Match.compare(X, new MatchBin(MY, MY, Ops.FAdd))) {
            return BinaryInstruction.create(Ops.FMul, Y, ConstantFP.get(3), InsertBefore);
        }
        // X + (X + X) -> 3*X
        if (Match.compare(Y, new MatchBin(MX, MX, Ops.FAdd))) {
            return BinaryInstruction.create(Ops.FMul, X, ConstantFP.get(3), InsertBefore);
        }
        ret = simplifyAssociative(Ops.FAdd, X, Y, recurse);
        return ret;
    }

    public static Value simplifySub(Value X, Value Y, int recurse) {
        Value ret = createSub(X, Y);
        if (ret != null) return ret;
        Match MY = Match.createMatch(Y);
        // X - X -> 0
        if (Match.compare(X, MY)) {
            return ConstantInt.get(0);
        }
        ret = simplifySubAssociative(Ops.Sub, X, Y, recurse);
        return ret;
    }

    public static Value simplifyFSub(Value X, Value Y, int recurse) {
        Value ret = createFSub(X, Y);
        if (ret != null) return ret;
        Match MY = Match.createMatch(Y);
        // X - X -> 0
        if (Match.compare(X, MY)) {
            return ConstantFP.get(0);
        }
        ret = simplifySubAssociative(Ops.FSub, X, Y, recurse);
        return ret;
    }

    public static Value simplifyMul(Value X, Value Y, int recurse) {
        Value ret = createMul(X, Y);
        if (ret != null) return ret;
        if (X instanceof Constant) {
            Value tmp = X;
            X = Y;
            Y = tmp;
        }
        Match MX = Match.createMatch(X), MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantInt.get(0));
        Match M1 = new MatchConst(ConstantInt.get(1));
        // X * 0 -> 0
        if (Match.compare(Y, M0)) {
            return ConstantInt.get(0);
        }
        // X * 1 -> X
        if (Match.compare(Y, M1)) {
            return X;
        }
        // (X / Y) * Y || Y * (X / Y) 需要考虑整除
//        MatchUndef MA=new MatchUndef(),MB=new MatchUndef();
//        if(Match.compare(X,new MatchBin(MA,MY,Ops.SDiv))
//                ||Match.compare(Y,new MatchBin(MA,MX,Ops.SDiv))){
//            return MA.V;
//        }

        ret = simplifyAssociative(Ops.Mul, X, Y, recurse);
        return ret;
    }

    public static Value simplifyFMul(Value X, Value Y, int recurse) {
        Value ret = createFMul(X, Y);
        if (ret != null) return ret;
        if (X instanceof Constant) {
            Value tmp = X;
            X = Y;
            Y = tmp;
        }
        Match MX = Match.createMatch(X), MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantInt.get(0));
        Match M1 = new MatchConst(ConstantInt.get(1));
        // X * 0 -> 0
        if (Match.compare(Y, M0)) {
            return ConstantFP.get(0);
        }
        // X * 1 -> X
        if (Match.compare(Y, M1)) {
            return X;
        }
        // (X / Y) * Y || Y * (X / Y)
        MatchUndef MA = new MatchUndef();
        if (Match.compare(X, new MatchBin(MA, MY, Ops.FDiv))
                || Match.compare(Y, new MatchBin(MA, MX, Ops.FDiv))) {
            return MA.V;
        }

        ret = simplifyAssociative(Ops.FMul, X, Y, recurse);
        return ret;
    }

    public static Value simplifyDiv(Value X, Value Y, int recurse) {
        Value ret = createSDiv(X, Y);
        if (ret != null) return ret;
        Match MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantInt.get(0));
        // 0 / X -> 0
        if (Match.compare(X, M0)) {
            return ConstantInt.get(0);
        }
        // X / X -> 1
        if (Match.compare(X, MY)) {
            return ConstantInt.get(1);
        }
        // X * Y / Y -> X
        MatchUndef MA = new MatchUndef();
        if (Match.compare(X, new MatchBin(MA, MY, Ops.Mul))) {
            return MA.V;
        }
        return ret;
    }

    public static Value simplifyFDiv(Value X, Value Y, int recurse) {
        Value ret = createFDiv(X, Y);
        if (ret != null) return ret;
        Match MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantInt.get(0));
        // 0 / X -> 0
        if (Match.compare(X, M0)) {
            return ConstantFP.get(0);
        }
        // X / X -> 1
        if (Match.compare(X, MY)) {
            return ConstantFP.get(1);
        }
        // X * Y / Y -> X
        MatchUndef MA = new MatchUndef();
        if (Match.compare(X, new MatchBin(MA, MY, Ops.FMul))) {
            return MA.V;
        }
        return ret;
    }

    public static Value simplifySRem(Value X, Value Y, int recurse) {
        Value ret = createSRem(X, Y);
        if (ret != null) return ret;
        Match MY = Match.createMatch(Y);
        Match M0 = new MatchConst(ConstantInt.get(0));
        // 0 % X -> 0
        if (Match.compare(X, M0)) {
            return ConstantInt.get(0);
        }
        // X % X -> 0
        if (Match.compare(X, MY)) {
            return ConstantInt.get(0);
        }
        // X * Y % Y -> 0
        MatchUndef MA = new MatchUndef();
        if (Match.compare(X, new MatchBin(MA, MY, Ops.Mul))) {
            return ConstantInt.get(0);
        }
        return ret;
    }

    /**
     * 结合律化简
     *
     * @param recurse 递归深度限制
     */
    public static Value simplifyAssociative(Ops Op, Value L, Value R, int recurse) {
        if (!Instruction.isAssociative(Op)) return null;
        if ((recurse--) == 0) return null;
        Instruction LI = null, RI = null;
        if (L instanceof Instruction) {
            LI = (Instruction) L;
        }
        if (R instanceof Instruction) {
            RI = (Instruction) R;
        }
        //(A op B) op C ==> A op (B op C)
        if (LI != null && LI.getOp() == Op) {
            Value A = LI.getOperand(0), B = LI.getOperand(1);
            Value V = simplifyBin(Op, B, R, recurse);
            if (V != null) {
                if (V == B) return L;
                Value W = simplifyBin(Op, A, V, recurse);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(Op, A, V, InsertBefore);
            }
        }
        //A op (B op C) ==> (A op B) op C
        if (RI != null && RI.getOp() == Op) {
            Value B = RI.getOperand(0), C = RI.getOperand(1);
            Value V = simplifyBin(Op, L, B, recurse);
            if (V != null) {
                if (V == B) return R;
                Value W = simplifyBin(Op, V, C, recurse);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(Op, V, C, InsertBefore);
            }
        }
        //若还支持交换律，则可以继续优化
        if (!Instruction.isCommutative(Op)) return null;
        //(A op B) op C ==> (C op A) op B
        if (LI != null && LI.getOp() == Op) {
            Value A = LI.getOperand(0), B = LI.getOperand(1);
            Value V = simplifyBin(Op, R, A, recurse);
            if (V != null) {
                if (V == A) return L;
                Value W = simplifyBin(Op, V, B, recurse);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(Op, V, B, InsertBefore);
            }
        }
        //A op (B op C) ==> B op (C op A)
        if (RI != null && RI.getOp() == Op) {
            Value B = RI.getOperand(0), C = RI.getOperand(1);
            Value V = simplifyBin(Op, C, L, recurse);
            if (V != null) {
                if (V == C) return R;
                Value W = simplifyBin(Op, B, V, recurse);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(Op, B, V, InsertBefore);
            }
        }
        return null;
    }

    /**
     * @param Op sub或FSub
     */
    public static Value simplifySubAssociative(Ops Op, Value L, Value R, int recurse) {
        if (recurse <= 0) return null;
        Instruction LI = null, RI = null;
        if (L instanceof Instruction) {
            LI = (Instruction) L;
        }
        if (R instanceof Instruction) {
            RI = (Instruction) R;
        }
        Ops add = (Op == Ops.Sub) ? Ops.Add : Ops.FAdd;
        // (X + Y) - Z -> X + (Y - Z) or Y + (X - Z)
        if (LI != null && LI.getOp() == add) { //Add或FAdd
            Value X = LI.getOperand(0), Y = LI.getOperand(1);
            Value V = simplifyBin(Op, Y, R, recurse - 1);
            if (V != null) {
                if (V == Y) return L;
                Value W = simplifyBin(add, X, V, recurse - 1);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(add, X, V, InsertBefore);
            }
            V = simplifyBin(Op, X, R, recurse - 1);
            if (V != null) {
                if (V == X) return L;
                Value W = simplifyBin(add, Y, V, recurse - 1);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(add, Y, V, InsertBefore);
            }
        }
        // X - (Y + Z) -> (X - Y) - Z or (X - Z) - Y
        if (RI != null && RI.getOp() == add) { //Add或FAdd
            Value Y = RI.getOperand(0), Z = RI.getOperand(1);
            Value V = simplifyBin(Op, L, Y, recurse - 1);
            if (V != null) {
                Value W = simplifyBin(Op, V, Z, recurse - 1);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(Op, V, Z, InsertBefore);
            }
            V = simplifyBin(Op, L, Z, recurse - 1);
            if (V != null) {
                Value W = simplifyBin(Op, V, Y, recurse - 1);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(Op, V, Y, InsertBefore);
            }
        }
        // Z - (X - Y) -> (Z - X) + Y
        if (RI != null && RI.getOp() == Op) { //Sub或FSub
            Value X = RI.getOperand(0), Y = RI.getOperand(1);
            Value V = simplifyBin(Op, L, X, recurse - 1);
            if (V != null) {
                Value W = simplifyBin(add, V, Y, recurse - 1);
                if (W != null) {
                    return W;
                }
                return BinaryInstruction.create(add, V, Y, InsertBefore);
            }
        }
        return null;
    }

    public static Value simplifyBin(Ops Op, Value L, Value R, int recurse) {
        if (L instanceof GlobalVariable) {
            L = ((GlobalVariable) L).getOperand(0);
        }
        if (R instanceof GlobalVariable) {
            R = ((GlobalVariable) R).getOperand(0);
        }
        switch (Op) {
            case Add -> {
                return simplifyAdd(L, R, recurse);
            }
            case FAdd -> {
                return simplifyFAdd(L, R, recurse);
            }
            case Sub -> {
                return simplifySub(L, R, recurse);
            }
            case FSub -> {
                return simplifyFSub(L, R, recurse);
            }
            case Mul -> {
                return simplifyMul(L, R, recurse);
            }
            case FMul -> {
                return simplifyFMul(L, R, recurse);
            }
            case SDiv -> {
                return simplifyDiv(L, R, recurse);
            }
            case FDiv -> {
                return simplifyFDiv(L, R, recurse);
            }
            case SRem -> {
                return simplifySRem(L, R, recurse);
            }
            default -> {
                return null;
            }
        }
    }

    public static Value simplifyPhi(PHIInst PI) {
        return PI.hasConstantValue(PassManager.ignoreUndef);
    }

    public static Value simplifyICmp(CmpInst.Predicate P, Value LHS, Value RHS) {
        if (LHS instanceof GlobalVariable) {
            LHS = ((GlobalVariable) LHS).getOperand(0);
        }
        if (RHS instanceof GlobalVariable) {
            RHS = ((GlobalVariable) RHS).getOperand(0);
        }
        if (LHS instanceof Constant) {
            if (RHS instanceof Constant) {
                return createIcmp(P, (Constant) LHS, (Constant) RHS);
            }
            Value tmp = LHS;
            LHS = RHS;
            RHS = tmp;
            P = CmpInst.getSwappedPre(P);
        }
        //icmp X,X
        if (LHS == RHS) return ConstantInt.get(Type.getInt1Ty(), CmpInst.trueWhenEqual(P) ? 1 : 0);
        return null;
    }

    public static Value simplifyFCmp(CmpInst.Predicate P, Value LHS, Value RHS) {
        if (LHS instanceof GlobalVariable) {
            LHS = ((GlobalVariable) LHS).getOperand(0);
        }
        if (RHS instanceof GlobalVariable) {
            RHS = ((GlobalVariable) RHS).getOperand(0);
        }
        if (LHS instanceof Constant) {
            if (RHS instanceof Constant) {
                return createFcmp(P, (Constant) LHS, (Constant) RHS);
            }
            Value tmp = LHS;
            LHS = RHS;
            RHS = tmp;
            P = CmpInst.getSwappedPre(P);
        }
        //Fcmp X,X
        if (LHS == RHS) return ConstantInt.get(Type.getInt1Ty(), CmpInst.trueWhenEqual(P) ? 1 : 0);
        return null;
    }

    public static Value simplifyCast(Ops Op, Value V, Type target) {
        if (V instanceof Constant) {
            return createCast(Op, (Constant) V, target);
        }
        return null;
    }

    //常量折叠

    public static Constant createIcmp(CmpInst.Predicate P, Constant LHS, Constant RHS) {
        if (!(LHS instanceof ConstantInt && RHS instanceof ConstantInt)) return null;
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
        if (!(LHS instanceof ConstantFP && RHS instanceof ConstantFP)) return null;
        assert Instructions.ICmpInst.isIntPredicate(P);

        Type ResultType = Type.getInt1Ty();
        ConstantFP L = (ConstantFP) LHS;
        ConstantFP R = (ConstantFP) RHS;

        switch (P) {
            case FCMP_UEQ:
                return ConstantInt.get(ResultType, L.getVal() == R.getVal() ? 1 : 0);
            case FCMP_UNE:
                return ConstantInt.get(ResultType, L.getVal() != R.getVal() ? 1 : 0);
            case FCMP_ULT:
                return ConstantInt.get(ResultType, L.getVal() < R.getVal() ? 1 : 0);
            case FCMP_UGT:
                return ConstantInt.get(ResultType, L.getVal() > R.getVal() ? 1 : 0);
            case FCMP_ULE:
                return ConstantInt.get(ResultType, L.getVal() <= R.getVal() ? 1 : 0);
            case FCMP_UGE:
                return ConstantInt.get(ResultType, L.getVal() >= R.getVal() ? 1 : 0);
        }
        return null;
    }

    public static Constant createCast(Instruction.Ops Op, Constant C, Type Ty) {
        switch (Op) {
            case ZExt -> {
                if (C instanceof ConstantInt) {
                    return ConstantInt.get(Ty, ((ConstantInt) C).getVal());
                }
                return null;
            }
            case SIToFP -> {
                if (C instanceof ConstantInt) {
                    float val = (float) ((ConstantInt) C).getVal();
                    return ConstantFP.get(val);
                }
                return null;
            }
            case FPToSI -> {
                if (C instanceof ConstantFP) {
                    int val = (int) ((ConstantFP) C).getVal();
                    return ConstantInt.get(val);
                }
                return null;
            }
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
        } else if (RHS instanceof ConstantFP) {
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

    public static Value createSRem(Value LHS, Value RHS) {
        return createBinOp(Ops.SRem, LHS, RHS);
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
//                TrueBlock.removePredecessor(BB);
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
