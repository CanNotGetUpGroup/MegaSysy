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
         * 不带类型
         * @return
         */
        @Override
        public String getName() {
            return String.valueOf(this.getVal());
        }

        @Override
        public String toString() {
            return (getType().isInt32Ty()?"i32 ":"i1 ") + this.getVal();
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

        @Override
        public String getName() {
            return String.valueOf(this.getVal());
        }

        @Override
        public String toString() {
            return "float " + this.getVal();
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
            int hash = hash(ty,V);
            if (context.ArrayConstants.containsKey(hash)) {
                return context.ArrayConstants.get(hash);
            }
            //存入MyContext
            ConstantArray tmp = new ConstantArray(ty,V);
            context.ArrayConstants.put(hash, tmp);
            return tmp;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getType().toString()).append(" ");
            if(isZero()){
                sb.append("zeroinitializer");
            }else{
                sb.append("[");
                int i;
                for (i = 0; i < getOperandList().size(); i++) {
                    sb.append(getOperandList().get(i).toString()).append(",");
                }
                while(i<this.getType().getNumElements()){
                    sb.append(Constant.getNullValue(this.getType().getKidType())).append(",");
                    i++;
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("]");
            }
            return sb.toString();
        }

        @Override
        public String getName() {
            StringBuilder sb = new StringBuilder();
            if(isZero()){
                sb.append("zeroinitializer");
            }else{
                sb.append("[");
                for (int i = 0; i < getOperandList().size(); i++) {
                    sb.append(getOperandList().get(i).toString()).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                sb.append("]");
            }
            return sb.toString();
        }

        public ArrayType getType(){
            return (ArrayType) super.getType();
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
//            if(!ty.getKidType().isArrayTy()){
//                for(int i=0;i<ty.getNumElements();i++){
//                    V.add(Constant.getNullValue(ty.getKidType()));
//                }
//            }else{
//                ConstantArray tmp=getZeroArr((ArrayType) ty.getKidType());
//                for(int i=0;i<ty.getNumElements();i++){
//                    V.add(tmp);
//                }
//            }
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

        public static int hash(ArrayType ty, ArrayList<Value> V) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ty.getHashcode();
            for(Value v:V){
                if(v instanceof ConstantInt||v instanceof ConstantFP){
                    result = prime * result + v.hashCode();
                }else if(v instanceof ConstantArray){
                    result = prime * result + hash(((ConstantArray) v).getType(),((ConstantArray) v).getArr());
                }
            }
            return result;
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
