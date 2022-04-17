package ir;

public abstract class Constants {
    /// Base class for constants with no operands.
    ///
    /// These constants have no operands; they represent their data directly.
    /// Since they can be in use by unrelated modules (and are never based on
    /// GlobalValues), it never makes sense to RAUW them.
    public static class ConstantData extends Constant {
        public ConstantData(Type ty, int numOps) {
            super(ty, numOps);
        }
    }

    //===----------------------------------------------------------------------===//
    /// This is the shared class of boolean and integer constants. This class
    /// represents both boolean and integral constants.
    /// Class for constant integers.
    public static class ConstantInt extends ConstantData {
        public ConstantInt(Type ty, int numOps) {
            super(ty, numOps);
        }
    }

    //===----------------------------------------------------------------------===//
    /// All zero aggregate value
    ///
    public static class ConstantAggregateZero extends ConstantData {
        public ConstantAggregateZero(Type ty, int numOps) {
            super(ty, numOps);
        }
    }

    /// Base class for aggregate constants (with operands).
    ///
    /// These constants are aggregates of other constants, which are stored as
    /// operands.
    ///
    /// Subclasses are \a ConstantStruct, \a ConstantArray, and \a
    /// ConstantVector.
    ///
    /// \note Some subclasses of \a ConstantData are semantically aggregates --
    /// such as \a ConstantDataArray -- but are not subclasses of this because they
    /// use operands.
    public static class ConstantAggregate extends Constant {
        public ConstantAggregate(Type ty, int numOps) {
            super(ty, numOps);
        }
    }

    //===----------------------------------------------------------------------===//
    /// ConstantArray - Constant Array Declarations
    ///
    public static class ConstantArray extends ConstantAggregate {
        public ConstantArray(Type ty, int numOps) {
            super(ty, numOps);
        }
    }


}
