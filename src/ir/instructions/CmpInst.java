package ir.instructions;

import ir.Instruction;
import ir.Type;
import ir.Value;

public class CmpInst extends Instruction {
    /// This enumeration lists the possible predicates for CmpInst subclasses.
    /// Values in the range 0-31 are reserved for FCmpInst, while values in the
    /// range 32-64 are reserved for ICmpInst. This is necessary to ensure the
    /// predicate values are not overlapping between the classes.
    ///
    /// Some passes (e.g. InstCombine) depend on the bit-wise characteristics of
    /// FCMP_* values. Changing the bit patterns requires a potential change to
    /// those passes.
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

    public CmpInst(Type type,Ops op, String name,  Predicate pre, Value LHS, Value RHS) {
        super(type,op,name, 2);
        super.setOperand(0,LHS);
        super.setOperand(1,RHS);
    }

    public CmpInst(Type type,Ops op,  Predicate pre, Value LHS, Value RHS) {
        super(type,op, 2);
        super.setOperand(0,LHS);
        super.setOperand(1,RHS);
    }

    public static boolean isFPPredicate(Predicate P){
        return P.compareTo(Predicate.FCMP_FALSE)>=0;
    }

    public static boolean isIntPredicate(Predicate P){
        return P.compareTo(Predicate.FCMP_FALSE)<0;
    }
}
