package ir;

import java.util.ArrayList;
import ir.DerivedTypes.*;

public class Type {
    public enum TypeID {
        FloatTyID,     ///< 32-bit floating point type
        VoidTyID,      ///< type with no size
        LabelTyID,     ///< Labels

        // Derived types... see DerivedTypes.java file.
        IntegerTyID,       ///< Arbitrary bit width integers
        FunctionTyID,      ///< Functions
        PointerTyID,       ///< Pointers
        ArrayTyID,         ///< Arrays
        FixedVectorTyID,   ///< Fixed width SIMD vector type
        ScalableVectorTyID ///< Scalable SIMD vector type
    }

    private TypeID ID=TypeID.LabelTyID;
    /// A pointer to the array of Types contained by this Type. For example, this
    /// includes the arguments of a function type, the elements of a structure,
    /// the pointee of a pointer, the element type of an array, etc. This pointer
    /// may be 0 for types that don't contain other types (Integer, Double,
    /// Float).
    private ArrayList<Type> containedTys;

    public Type(TypeID tid) {
        ID=tid;
    }

    public TypeID getID() {
        return ID;
    }

    public void setID(TypeID ID) {
        this.ID = ID;
    }

    public ArrayList<Type> getContainedTys() {
        return containedTys;
    }

    public void setContainedTys(ArrayList<Type> containedTys) {
        this.containedTys = containedTys;
    }

    /**
     * factory functions
     */
    public static IntegerType getIntegerTy(){
        return MyContext.IntegerTy;
    }
    public static Type getFloatTy(){
        return MyContext.FloatTy;
    }
    public static Type getVoidTy(){
        return MyContext.VoidTy;
    }
    public static Type getArrayTy(Type ElementType, int numElements){
        return ArrayType.get(ElementType,  numElements);
    }
    public static Type getFunctionTy(Type ReturnType, ArrayList<Type> Params){
        return FunctionType.get(ReturnType,  Params);
    }
    public static Type getFunctionTy(Type ReturnType){
        return FunctionType.get(ReturnType);
    }

    /**
     * judge functions
     */
    /// Return true if this is 'label'.
    boolean isLabelTy() { return getID() == TypeID.LabelTyID; }
    /// True if this is an instance of IntegerType.
    boolean isIntegerTy() { return getID() == TypeID.IntegerTyID; }
    /// Return true if this is an integer type or a pointer type.
    boolean isIntOrPtrTy() { return isIntegerTy() || isPointerTy(); }
    /// True if this is an instance of FunctionType.
    boolean isFunctionTy() { return getID() == TypeID.FunctionTyID; }
    /// True if this is an instance of ArrayType.
    boolean isArrayTy() { return getID() == TypeID.ArrayTyID; }
    /// True if this is an instance of PointerType.
    boolean isPointerTy() { return getID() == TypeID.PointerTyID; }
    /// True if this is an instance of VectorType.
    boolean isVectorTy() {
        return getID() == TypeID.ScalableVectorTyID || getID() == TypeID.FixedVectorTyID;
    }
}
