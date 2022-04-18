package ir;

import java.util.ArrayList;

public abstract class DerivedTypes {
    public static MyContext context=MyContext.getInstance();
    /// Integer representation type
    ///
    public static class IntegerType extends Type {
        public IntegerType() {
            super(TypeID.IntegerTyID);
        }
    }

    /// Class to represent array types.
    ///
    public static class ArrayType extends Type {
        private int NumElements;

        private ArrayType(Type ElementType,int numElements) {
            super(TypeID.ArrayTyID);
            getContainedTys().add(ElementType);
            this.NumElements=numElements;
            //TODO:存入MyContext
        }

        public int getNumElements() {
            return NumElements;
        }

        public void setNumElements(int numElements) {
            NumElements = numElements;
        }

        public static ArrayType get(Type ElementType, int numElements){
            //TODO:检查MyContext中是否已经存在这种ArrayType，存在则直接返回该Type
            return new ArrayType(ElementType,numElements);
        }
    }

    /// Class to represent function types
    ///
    public static class FunctionType extends Type {
        public FunctionType() {
            super(TypeID.FunctionTyID);
        }

        private FunctionType(Type ReturnType, ArrayList<Type> Params) {
            super(TypeID.FunctionTyID);
            ArrayList<Type> contain = getContainedTys();
            contain.add(ReturnType);
            if (Params != null)
                contain.addAll(Params);
            //TODO:存入MyContext
        }

        /**
         * This is the factory function for the FunctionType class.
         */
        public static FunctionType get(Type ReturnType, ArrayList<Type> Params) {
            //TODO:检查MyContext中是否已经存在这种FunctionType，存在则直接返回该Type
            return new FunctionType(ReturnType, Params);
        }

        public static FunctionType get(Type ReturnType) {
            //TODO:检查MyContext中是否已经存在这种FunctionType，存在则直接返回该Type
            return new FunctionType(ReturnType, null);
        }

    }


}
