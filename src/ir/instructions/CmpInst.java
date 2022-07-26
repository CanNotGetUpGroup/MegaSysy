package ir.instructions;

import ir.Instruction;
import ir.Type;
import ir.Value;

public abstract class CmpInst extends Instruction {
    public enum Predicate {
        ICMP_EQ,  ///< equal
        ICMP_NE,  ///< not equal
        ICMP_SGT, ///< signed greater than
        ICMP_SGE, ///< signed greater or equal
        ICMP_SLT, ///< signed less than
        ICMP_SLE, ///< signed less or equal
        FCMP_FALSE, ///< Always false (always folded)
        FCMP_OEQ,   ///< True if ordered and equal
        FCMP_OGT,   ///< True if ordered and greater than
        FCMP_OGE,   ///< True if ordered and greater than or equal
        FCMP_OLT,   ///< True if ordered and less than
        FCMP_OLE,   ///< True if ordered and less than or equal
        FCMP_ONE,   ///< True if ordered and operands are unequal
        FCMP_ORD,   ///< True if ordered (no nans)
        FCMP_UNO,   ///< True if unordered: isnan(X) | isnan(Y)
        FCMP_UEQ,   ///< True if unordered or equal
        FCMP_UGT,  ///< True if unordered or greater than
        FCMP_UGE,  ///< True if unordered, greater than, or equal
        FCMP_ULT,  ///< True if unordered or less than
        FCMP_ULE,  ///< True if unordered, less than, or equal
        FCMP_UNE,  ///< True if unordered or not equal
        FCMP_TRUE, ///< Always true (always folded)
    }

    private Predicate predicate;

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public CmpInst(Type type, Ops op, String name, Predicate pre, Value LHS, Value RHS) {
        super(type,op,name, 2);
        addOperand(LHS);
        addOperand(RHS);
        predicate=pre;
    }

    public CmpInst(Type type,Ops op,  Predicate pre, Value LHS, Value RHS) {
        super(type,op, 2);
        addOperand(LHS);
        addOperand(RHS);
        predicate=pre;
    }

    public static boolean isFPPredicate(Predicate P){
        return P.compareTo(Predicate.FCMP_FALSE)>=0;
    }

    public static boolean isIntPredicate(Predicate P){
        return P.compareTo(Predicate.FCMP_FALSE)<0;
    }
}
