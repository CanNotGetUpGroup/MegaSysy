package ir.instructions;

import ir.Instruction;
import ir.Type;
import ir.Value;

public abstract class Instructions {
    //===----------------------------------------------------------------------===//
    //                                AllocaInst Class
    //===----------------------------------------------------------------------===//

    /// an instruction to allocate memory on the stack
    public static class AllocaInst extends UnaryInstruction {
        public AllocaInst(Type type, String name, int numOperands) {
            super(type, name, numOperands);
        }

    }

    //===----------------------------------------------------------------------===//
    //                                LoadInst Class
    //===----------------------------------------------------------------------===//

    /// An instruction for reading from memory. This uses the SubclassData field in
    /// Value to store whether or not the load is volatile.
    public static class LoadInst extends UnaryInstruction{
        public LoadInst(Type type, String name, int numOperands) {
            super(type, name, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                StoreInst Class
    //===----------------------------------------------------------------------===//

    /// An instruction for storing to memory.
    public static class StoreInst extends Instruction {
        public StoreInst(Type type, String name, int numOperands) {
            super(type, name, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                                FenceInst Class
    //===----------------------------------------------------------------------===//

    /// An instruction for ordering other memory operations.
    public static class FenceInst extends Instruction {
        public FenceInst(Type type, String name, int numOperands) {
            super(type, name, numOperands);
        }
    }

    //===----------------------------------------------------------------------===//
    //                             GetElementPtrInst Class
    //===----------------------------------------------------------------------===//

    // checkGEPType - Simple wrapper function to give a better assertion failure
    // message on bad indexes for a gep instruction.
    //
    public static class GetElementPtrInst extends Instruction {
        public GetElementPtrInst(Type type, String name, int numOperands) {
            super(type, name, numOperands);
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
            super(LHS.getType(),name,pred,LHS,RHS);
        }
    }
}
