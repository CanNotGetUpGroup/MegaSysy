package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ir.Constants.*;
import ir.DerivedTypes.*;

public class MyContext {
    private static final MyContext myContext = new MyContext();

    private MyContext() {
        IntConstants = new HashMap<>();
        FPConstants = new HashMap<>();
        ArrayConstants=new HashMap<>();
        arrayTypes = new HashMap<>();
        functionTypes = new HashMap<>();
        pointerTypes = new HashMap<>();
    }

    /**
     * types instance
     */
    public static final IntegerType Int32Ty = new DerivedTypes.IntegerType(0);
    public static final IntegerType Int1Ty = new DerivedTypes.IntegerType(1);
    public static final Type FloatTy = new Type(Type.TypeID.FloatTyID,2);
    public static final Type VoidTy = new Type(Type.TypeID.VoidTyID,3);
    public static final Type LabelTy = new Type(Type.TypeID.LabelTyID,4);

    public HashMap<Integer, ArrayType> arrayTypes;
    public HashMap<Integer, FunctionType> functionTypes;
    public HashMap<Integer, PointerType> pointerTypes;


    /**
     * 常量储存器
     */
    public HashMap<Integer, ConstantInt> IntConstants;
    public HashMap<Float, ConstantFP> FPConstants;
    public HashMap<Integer, ConstantArray> ArrayConstants;
    public ConstantInt int1_0;
    public ConstantInt int1_1;

    /**
     * value命名序号
     */
    public static int valuePtr = 0;

    public static MyContext getInstance() {
        return myContext;
    }


}
