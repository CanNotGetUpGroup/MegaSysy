package ir;

import ir.DerivedTypes.*;

import java.util.ArrayList;

public class Constants {
    public static MyContext context = MyContext.getInstance();

    /// Base class for constants with no operands.
    ///
    /// These constants have no operands; they represent their data directly.
    /// Since they can be in use by unrelated modules (and are never based on
    /// GlobalValues), it never makes sense to RAUW them.
    public static class ConstantData extends Constant {
        public ConstantData(Type ty) {
            super(ty, 0);
        }
    }

    //===----------------------------------------------------------------------===//
    /// This is the shared class of boolean and integer constants. This class
    /// represents both boolean and integral constants.
    /// Class for constant integers.
    public static class ConstantInt extends ConstantData {
        private int val;

        public ConstantInt(Type ty, int val) {
            super(ty);
            this.val = val;
            if(ty.isInt32Ty()) context.IntConstants.put(val, this);
        }

        /**
         * 获取一个值为v的ConstantInt对象，若MyContext中已存在，则直接返回
         */
        public static ConstantInt get(int v) {
            return context.IntConstants.getOrDefault(v, new ConstantInt(Type.getInt32Ty(), v));
        }

        public static ConstantInt get(Type ty, int v) {
            if (ty.equals(Type.getInt1Ty())) {
                if (v == 0){
                    if(context.int1_0==null){
                        context.int1_0=new ConstantInt(Type.getInt1Ty(), 0);
                    }
                    return context.int1_0;
                }
                else{
                    if(context.int1_1==null){
                        context.int1_1=new ConstantInt(Type.getInt1Ty(), 1);
                    }
                    return context.int1_1;
                }
            }
            return context.IntConstants.getOrDefault(v, new ConstantInt(ty, v));
        }

        /**
         * 常量0
         */
        public static ConstantInt const_0() {
            return get(Type.getInt32Ty(), 0);
        }

        public static ConstantInt const1_0() {
            return get(Type.getInt1Ty(), 0);
        }

        public static ConstantInt const1_1() {
            return get(Type.getInt1Ty(), 1);
        }

        public boolean isZero() {return val==0;}

        public int getVal() {
            return val;
        }

        public void setVal(int val) {
            this.val = val;
        }

        public void destroy() {
            context.IntConstants.remove(this.val);
        }
    }

    //===----------------------------------------------------------------------===//
    /// ConstantFP - Floating Point Values [float, double]
    ///
    public static class ConstantFP extends ConstantData {
        private float val;

        private ConstantFP(Type ty, float val) {
            super(ty);
            this.val = val;
            context.FPConstants.put(val, this);
        }

        /**
         * 获取一个值为v的ConstantFP对象，若MyContext中已存在，则直接返回
         */
        public static ConstantFP get(float v) {
            return context.FPConstants.getOrDefault(v, new ConstantFP(Type.getFloatTy(), v));
        }

        /**
         * 浮点数常量0
         */
        public static ConstantFP const_0() {
            return get(0);
        }

        public boolean isZero() {return val==0;}

        public float getVal() {
            return val;
        }

        public void setVal(float val) {
            this.val = val;
        }

        public void destroy() {
            context.FPConstants.remove(this.val);
        }
    }

    //===----------------------------------------------------------------------===//
    /// All zero aggregate value
    ///
    public static class ConstantAggregateZero extends ConstantData {
        private ConstantAggregateZero(Type ty) {
            super(ty);
            //TODO:存入MyContext
        }

        public static ConstantAggregateZero get(Type ty) {
            assert ty.isArrayTy() || ty.isVectorTy();
            //TODO:检查是否存在
            return null;
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
        private ConstantAggregate(Type ty, ArrayList<Value> V) {
            super(ty, V);
            for (Value v : V) {
                assert ty == v.getType();
            }
            //TODO:存入MyContext
        }

        public static ConstantAggregate get(Type ty, ArrayList<Value> V) {
            //TODO:检查是否存在
            return new ConstantAggregate(ty, V);
        }
    }

    //===----------------------------------------------------------------------===//
    /// ConstantArray - Constant Array Declarations
    ///
    public static class ConstantArray extends Constant {
        public ConstantArray(ArrayType ty, ArrayList<Value> V) {
            super(ty, V);
            //TODO:存入MyContext
        }

        public static ConstantArray get(ArrayType ty, ArrayList<Value> V) {
            for (Value v : V) {
                assert v instanceof Constant;
            }
            //TODO:检查是否存在
            return null;
        }

        public ArrayType getArrType(){
            return (ArrayType) getType();
        }

        public ArrayList<Value> getArr(){
            return getOperandList();
        }

        /**
         * 生成类型为ty的全0数组
         * @param ty
         * @return
         */
        public static ConstantArray getZeroArr(ArrayType ty){
            ArrayList<Value> V=new ArrayList<>();
            if(!ty.getKidType().isArrayTy()){
                for(int i=0;i<ty.getNumElements();i++){
                    V.add(Constant.getNullValue(ty.getKidType()));
                }
            }else{
                ConstantArray tmp=getZeroArr((ArrayType) ty.getKidType());
                for(int i=0;i<ty.getNumElements();i++){
                    V.add(tmp);
                }
            }
            return get(ty,V);
        }

        public boolean isZero() {
            for(Value v:getArr()){
                if(v instanceof ConstantInt || v instanceof ConstantFP){
                    if(!((Constant)v).isNullValue()){
                        return false;
                    }
                }else if(v instanceof ConstantArray){
                    if(!((ConstantArray)v).isZero()){
                        return false;
                    }
                }else {
                    return false;
                }
            }
            return true;
        }
    }

    //===----------------------------------------------------------------------===//
    /// A constant value that is initialized with an expression using
    /// other constant values.
    ///
    /// This class uses the standard Instruction opcodes to define the various
    /// constant expressions.  The Opcode field for the ConstantExpr class is
    /// maintained in the Value::SubclassData field.
    public static class ConstantExpr extends Constant {
        public ConstantExpr(Type ty) {
            super(ty);
        }

    }
}
