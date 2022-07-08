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
    private ArrayList<Type> containedTys=new ArrayList<>();
    private int hashcode;

    public int getHashcode() {
        return hashcode;
    }

    public void setHashcode(int hashcode) {
        this.hashcode = hashcode;
    }

    public Type(TypeID tid,int hashcode) {
        ID=tid;
        this.hashcode=hashcode;
    }

    @Override
    public String toString() {
        switch (ID){
            case VoidTyID -> {
                return "void";
            }
            case FloatTyID -> {
                return "float";
            }
            case LabelTyID -> {
                return "label";
            }
        }
        return "undef";
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

    public Type getContainedTys(int i) {
        return containedTys.get(i);
    }

    public void setContainedTys(ArrayList<Type> containedTys) {
        this.containedTys = containedTys;
    }

    /**
     * factory functions
     */
    public static Type getInt32Ty(){
        return MyContext.Int32Ty;
    }
    public static Type getInt1Ty(){
        return MyContext.Int1Ty;
    }
    public static Type getFloatTy(){
        return MyContext.FloatTy;
    }
    public static Type getVoidTy(){
        return MyContext.VoidTy;
    }
    public static Type getLabelTy() {return MyContext.LabelTy;}
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
    public boolean isLabelTy() { return getID() == TypeID.LabelTyID; }
    /// True if this is an instance of IntegerType.
    public boolean isIntegerTy() { return getID() == TypeID.IntegerTyID; }
    /// True if this is an instance of FloatType.
    public boolean isFloatTy() { return getID() == TypeID.FloatTyID; }
    /// True if this is an instance of VoidType.
    public boolean isVoidTy() { return getID() == TypeID.VoidTyID; }
    /// Return true if this is an integer type or a pointer type.
    public boolean isIntOrPtrTy() { return isIntegerTy() || isPointerTy(); }
    /// True if this is an instance of FunctionType.
    public boolean isFunctionTy() { return getID() == TypeID.FunctionTyID; }
    /// True if this is an instance of ArrayType.
    public boolean isArrayTy() { return getID() == TypeID.ArrayTyID; }
    /// True if this is an instance of PointerType.
    public boolean isPointerTy() { return getID() == TypeID.PointerTyID; }
    /// True if this is an instance of VectorType.
    public boolean isVectorTy() {
        return getID() == TypeID.ScalableVectorTyID || getID() == TypeID.FixedVectorTyID;
    }

    public boolean isInt1Ty() {return equals(getInt1Ty());}

    public boolean isInt32Ty() {return equals(getInt32Ty());}
}
