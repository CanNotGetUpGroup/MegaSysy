package ir.instructions;

import ir.Instruction;
import ir.Type;
import ir.Value;

public abstract class CmpInst extends BinaryInstruction {
    public enum Predicate {
        ICMP_EQ,  ///< equal
        ICMP_NE,  ///< not equal
        ICMP_SGT, ///< signed greater than
        ICMP_SGE, ///< signed greater or equal
        ICMP_SLT, ///< signed less than
        ICMP_SLE, ///< signed less or equal
        FCMP_FALSE, ///< Always false (always folded)
        FCMP_UEQ,   ///< True if unordered or equal
        FCMP_UGT,  ///< True if unordered or greater than
        FCMP_UGE,  ///< True if unordered, greater than, or equal
        FCMP_ULT,  ///< True if unordered or less than
        FCMP_ULE,  ///< True if unordered, less than, or equal
        FCMP_UNE,  ///< True if unordered or not equal
        FCMP_TRUE, ///< Always true (always folded)
    }

    public static Predicate getSwappedPre(Predicate P){
        return switch (P){
            case ICMP_EQ -> Predicate.ICMP_EQ;
            case ICMP_NE -> Predicate.ICMP_NE;
            case ICMP_SGT -> Predicate.ICMP_SLT;
            case ICMP_SGE -> Predicate.ICMP_SLE;
            case ICMP_SLT -> Predicate.ICMP_SGT;

            case ICMP_SLE -> Predicate.ICMP_SGE;
            case FCMP_FALSE -> Predicate.FCMP_FALSE;
            case FCMP_UEQ -> Predicate.FCMP_UEQ;
            case FCMP_UGT -> Predicate.FCMP_ULT;
            case FCMP_UGE -> Predicate.FCMP_ULE;
            case FCMP_ULT -> Predicate.FCMP_UGT;
            case FCMP_ULE -> Predicate.FCMP_UGE;
            case FCMP_UNE -> Predicate.FCMP_UNE;
            case FCMP_TRUE -> Predicate.FCMP_TRUE;
        };
    }

    public static boolean trueWhenEqual(Predicate P){
        return switch (P) {
            case ICMP_EQ, ICMP_SGE, ICMP_SLE, FCMP_UEQ, FCMP_UGE, FCMP_ULE -> true;
            default -> false;
        };
    }

    private Predicate predicate;

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public CmpInst(Type type, Ops op, String name, Predicate pre, Value LHS, Value RHS) {
        super(type,op,name,LHS,RHS);
        predicate=pre;
    }

    public CmpInst(Type type,Ops op,  Predicate pre, Value LHS, Value RHS) {
        super(type,op, LHS,RHS);
        predicate=pre;
    }

    public static boolean isFPPredicate(Predicate P){
        return P.compareTo(Predicate.FCMP_FALSE)>=0;
    }

    public static boolean isIntPredicate(Predicate P){
        return P.compareTo(Predicate.FCMP_FALSE)<0;
    }
}
