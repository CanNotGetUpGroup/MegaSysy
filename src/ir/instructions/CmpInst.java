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
    }

    public CmpInst(Type type, String name,  Predicate pre, Value LHS, Value RHS) {
        super(type,name, 2);
        super.setOperand(0,LHS);
        super.setOperand(1,RHS);
    }

    public CmpInst(Type type,  Predicate pre, Value LHS, Value RHS) {
        super(type, 2);
        super.setOperand(0,LHS);
        super.setOperand(1,RHS);
    }
}
