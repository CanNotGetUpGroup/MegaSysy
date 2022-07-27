package ir;

import java.util.ArrayList;

/**
 * 生成复杂类型对象统一使用get方法，每个类型会根据信息生成hashcode，
 * 储存在MyContext中，避免重复生成相同的类型对象
 */
public abstract class DerivedTypes {
    public static MyContext context = MyContext.getInstance();

    /// Integer representation type
    ///
    public static class IntegerType extends Type {
        public IntegerType(int hashcode) {
            super(TypeID.IntegerTyID, hashcode);
        }

        @Override
        public String toString() {
            return this.isInt32Ty()?"i32":"i1";
        }
    }

    /// Class to represent array types.
    ///
    /**
     * e.g a[4][2]:NumElements=4,dim=2,eleSize=2
     */
    public static class ArrayType extends Type {
        private int NumElements;    //元素数量
        private int dim;            //维数
        private int eleSize;        //元素大小

        private ArrayType(Type ElementType, int numElements, int hashcode) {
            super(TypeID.ArrayTyID, hashcode);
            getContainedTys().add(ElementType);
            this.NumElements = numElements;
            if (ElementType.isIntegerTy() || ElementType.isFloatTy()) {
                dim = 1;
                eleSize = 1;
            } else if (ElementType.isArrayTy()) {
                ArrayType eleTy = ((ArrayType) ElementType);
                dim = eleTy.getDim() + 1;
                eleSize = eleTy.getEleSize() * eleTy.getNumElements();
            }
        }

        @Override
        public String toString() {
            return "[" + NumElements + " x " + getKidType().toString() + "]";
        }

        public int getNumElements() {
            return NumElements;
        }

        public void setNumElements(int numElements) {
            NumElements = numElements;
        }

        public int getDim() {
            return dim;
        }

        public void setDim(int dim) {
            this.dim = dim;
        }

        public int getEleSize() {
            return eleSize;
        }

        public void setEleSize(int eleSize) {
            this.eleSize = eleSize;
        }

        public Type getKidType() {
            return getContainedTys().get(0);
        }

        public boolean isIntArray(){
            if(getKidType().isArrayTy()){
                return ((ArrayType)getKidType()).isIntArray();
            }else{
                return getKidType().isInt32Ty();
            }
        }

        public boolean isFloatArray(){
            if(getKidType().isArrayTy()){
                return ((ArrayType)getKidType()).isFloatArray();
            }else{
                return getKidType().isFloatTy();
            }
        }

        /**
         * 数组尺寸
         */
        public int size() {
            return getNumElements()*getEleSize();
        }

        /**
         * 获取一个数组类型
         *
         * @param ElementType 元素类型
         * @param numElements 数组元素数量
         */
        public static ArrayType get(Type ElementType, int numElements) {
            //检查MyContext中是否已经存在这种ArrayType，存在则直接返回该Type
            int hash = hash(ElementType, numElements);
            if (context.arrayTypes.containsKey(hash)) {
                return context.arrayTypes.get(hash);
            }
            //存入MyContext
            ArrayType tmp = new ArrayType(ElementType, numElements, hash);
            context.arrayTypes.put(hash, tmp);
            return tmp;
        }

        public static int hash(Type ElementType, int numElements) {
            final int prime = 31;
            int result = 1;
            result = prime * result + numElements;
            if (ElementType.isIntegerTy() || ElementType.isFloatTy()) {
                result = prime * result + 1;
                result = prime * result + 1;
                result = prime * result + ElementType.getHashcode();
            } else if (ElementType.isArrayTy()) {
                result = prime * result + ((ArrayType) ElementType).getDim() + 1;
                result = prime * result + ((ArrayType) ElementType).getEleSize() * ((ArrayType) ElementType).getNumElements();
                result = prime * result + hash(((ArrayType) ElementType).getKidType(), ((ArrayType) ElementType).getNumElements());
            }
            return result;
        }
    }

    /// Class to represent function types
    /// 函数和参数类型储存在contain中
    public static class FunctionType extends Type {
        public FunctionType(int hashcode) {
            super(TypeID.FunctionTyID, hashcode);
        }

        private FunctionType(Type ReturnType, ArrayList<Type> Params, int hashcode) {
            super(TypeID.FunctionTyID, hashcode);
            ArrayList<Type> contain = getContainedTys();
            contain.add(ReturnType);
            if (Params != null)
                contain.addAll(Params);
        }

        @Override
        public String toString(){
            StringBuilder sb=new StringBuilder(getReturnType().toString());
            sb.append(" f(");
            for(int i=1;i<getContainedTys().size();i++){
                sb.append(getContainedTys(i)).append(" ,");
            }
            sb.append(")");
            return sb.toString();
        }

        /**
         * 获取一个函数类型
         *
         * @param ReturnType 函数返回类型
         * @param Params     参数类型
         */
        public static FunctionType get(Type ReturnType, ArrayList<Type> Params) {
            if (Params == null) {
                return get(ReturnType);
            }
            //检查MyContext中是否已经存在这种FunctionType，存在则直接返回该Type
            int hash = hash(ReturnType, Params);
            if (context.functionTypes.containsKey(hash)) {
                return context.functionTypes.get(hash);
            }
            FunctionType tmp = new FunctionType(ReturnType, Params, hash);
            context.functionTypes.put(hash, tmp);
            return tmp;
        }

        public static FunctionType get(Type ReturnType) {
            //检查MyContext中是否已经存在这种FunctionType，存在则直接返回该Type
            int hash = hash(ReturnType, null);
            if (context.functionTypes.containsKey(hash)) {
                return context.functionTypes.get(hash);
            }
            FunctionType tmp = new FunctionType(ReturnType, null, hash);
            context.functionTypes.put(hash, tmp);
            return tmp;
        }

        public Type getParamType(int i) {
            return getContainedTys().get(i + 1);
        }

        public int getParamNum(){
            return getContainedTys().size()-1;
        }

        public Type getReturnType() {
            return getContainedTys().get(0);
        }

        public static int hash(Type ReturnType, ArrayList<Type> Params) {
            final int prime = 31;
            int result = 1;
            result = prime * result + ReturnType.getHashcode();
            if (Params != null) {
                for (Type type : Params) {
                    result = prime * result + type.getHashcode();
                }
            }
            return result;
        }

    }

    /// Class to represent pointers.
    public static class PointerType extends Type {
        private Type PointeeTy;

        public PointerType(int hashcode) {
            super(TypeID.PointerTyID, hashcode);
        }

        private PointerType(Type pointType, int hashcode) {
            super(TypeID.PointerTyID, hashcode);
            ArrayList<Type> contain = getContainedTys();
            contain.add(pointType);
            PointeeTy = pointType;
        }

        @Override
        public String toString() {
            return PointeeTy.toString() + "*";
        }

        public static PointerType get(Type pointType) {
            //检查MyContext中是否已经存在这种PointerType，存在则直接返回该Type
            int hash = hash(pointType);
            if (context.pointerTypes.containsKey(hash)) {
                return context.pointerTypes.get(hash);
            }
            PointerType tmp = new PointerType(pointType, hash);
            context.pointerTypes.put(hash, tmp);
            return tmp;
        }

        public Type getElementType() {
            return PointeeTy;
        }

        public static int hash(Type pointType) {
            final int prime = 31;
            int result = 1;
            result = prime * result + pointType.getHashcode();
            return result;
        }
    }


}
