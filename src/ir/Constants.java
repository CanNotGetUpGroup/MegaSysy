package ir;

import ir.DerivedTypes.*;
import util.CloneMap;

import java.util.ArrayList;

public class Constants {
    public static MyContext context = MyContext.getInstance();

    public static abstract class ConstantData extends Constant {
        public ConstantData(Type ty) {
            super(ty, 0);
        }
    }

    //===----------------------------------------------------------------------===//
    /// 32 bits Integer and boolean
    public static class ConstantInt extends ConstantData {
        private int val;

        public ConstantInt(Type ty, int val) {
            super(ty);
            this.val = val;
            if(ty.isInt32Ty()) context.IntConstants.put(val, this);
            context.Constant2Hash.put(this, val);
        }

        /**
         * 不带类型
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
            if(context.IntConstants.containsKey(v)){
                return context.IntConstants.get(v);
            }
            return new ConstantInt(Type.getInt32Ty(),v);
//            return context.IntConstants.getOrDefault(v, new ConstantInt(Type.getInt32Ty(), v));
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
            if(context.IntConstants.containsKey(v)){
                return context.IntConstants.get(v);
            }
            return new ConstantInt(ty,v);
//            return context.IntConstants.getOrDefault(v, new ConstantInt(ty, v));
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

        @Override
        public Value copy(CloneMap cloneMap) {
            return this;
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
            context.Constant2Hash.put(this,Float.valueOf(val).hashCode());
        }

        @Override
        public String getName() {
            long bits=Double.doubleToLongBits(this.getVal());
            return "0x"+Long.toHexString(bits);
        }

        @Override
        public String toString() {
            return "float " + this.getVal();
        }

        /**
         * 获取一个值为v的ConstantFP对象，若MyContext中已存在，则直接返回
         */
        public static ConstantFP get(float v) {
            if(context.FPConstants.containsKey(v)){
                return context.FPConstants.get(v);
            }
            return new ConstantFP(Type.getFloatTy(), v);
//            return context.FPConstants.getOrDefault(v, new ConstantFP(Type.getFloatTy(), v));
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

        @Override
        public Value copy(CloneMap cloneMap) {
            return this;
        }
    }

    //===----------------------------------------------------------------------===//
    /// ConstantArray - Constant Array Declarations
    ///
    public static class ConstantArray extends Constant {
        public ConstantArray(ArrayType ty, ArrayList<Value> V) {
            super(ty, V);
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
            context.Constant2Hash.put(tmp,hash);
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
         * 获取a[i]
         */
        public Value getElement(int i){
            if(i>=getNumOperands()){
                return Constant.getNullValue(getType().getKidType());
            }
            return getOperand(i);
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

        @Override
        public Value copy(CloneMap cloneMap) {
            return this;
        }

        public ArrayList<Integer> getDims() { 
            ArrayList<Integer> ret = new ArrayList<>();
            ConstantArray cur = this;
            while(true) { 
                ret.add(cur.getArr().size());
                // 添加短路条件 zeroinit？
                if(cur.getArr().size()==0 || !(cur.getArr().get(0) instanceof ConstantArray)) {
                    break;
                }
                cur = (ConstantArray) cur.getArr().get(0);
            }
            return ret;
        }
    }

    public static class UndefValue extends Constant {
        public UndefValue(Type ty) {
            super(ty);
            context.UndefConstants.put(ty,this);
            context.Constant2Hash.put(this,ty.getHashcode()*31+1);
        }

        @Override
        public String getName() {
            return "undef";
        }

        @Override
        public String toString() {
            return getType() + " undef";
        }

        public static UndefValue get(Type ty){
            if(context.UndefConstants.containsKey(ty)){
                return context.UndefConstants.get(ty);
            }
            return new UndefValue(ty);
//            return context.UndefConstants.getOrDefault(ty, new UndefValue(ty));
        }

        public static boolean isUndefValue(Value V){
            return V instanceof UndefValue;
        }

        @Override
        public Value copy(CloneMap cloneMap) {
            return this;
        }
    }
}
