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
                if(ty.isInt32Ty())
                    return Constants.ConstantInt.const_0();
                else
                    return Constants.ConstantInt.const1_0();
            case FloatTyID:
                return Constants.ConstantFP.const_0();
            case ArrayTyID:
                return ConstantArray.getZeroArr((DerivedTypes.ArrayType) ty);
            default:
                return null;
        }
    }

    public static Constant getAllOnesValue(Type ty){
        switch (ty.getID()){
            case IntegerTyID:
                if(ty.isInt32Ty())
                    return Constants.ConstantInt.get(-1);
                else
                    return Constants.ConstantInt.const1_1();
            default:
                return null;
        }
    }

    public boolean isNullValue(){
        if(this instanceof ConstantInt){
            return ((ConstantInt)this).isZero();
        }
        if(this instanceof ConstantFP){
            return ((ConstantFP)this).isZero();
        }
        if(this instanceof ConstantArray){
            return ((ConstantArray)this).isZero();
        }
        return false;
    }
}
