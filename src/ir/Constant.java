package ir;
import ir.Constants.*;

import java.util.ArrayList;

public abstract class Constant extends User {
    public Constant(Type ty) {
        super(ty,0);
    }
    public Constant(Type ty,int numOps) {
        super(ty, numOps);
    }

    public Constant(Type ty, String name, int numOperands) {
        super(ty, name, numOperands);
    }

    public Constant(Type ty, Use Ops, int numOperands) {
        super(ty, Ops, numOperands);
    }

    public Constant(Type ty, String name) {
        super(ty, name);
    }

    public Constant(Type type, ArrayList<Value> operandList) {
        super(type, operandList);
    }

    /**
     * Constructor to create a '0' constant of arbitrary type.
     */
    public static Constant getNullValue(Type ty){
        switch (ty.getID()){
            case IntegerTyID:
                return Constants.ConstantInt.const_0();
            case FloatTyID:
                return Constants.ConstantFP.const_0();
            default:
                return null;
        }
    }
}
