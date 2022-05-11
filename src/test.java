import ir.*;
import ir.Module;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;

import java.util.ArrayList;
import java.util.HashMap;

public class test {
    public static void main(String[] args) {
        MyContext context = MyContext.getInstance();
        Module module=Module.getInstance();
        DerivedTypes.ArrayType tmp1=DerivedTypes.ArrayType.get(Type.getInt32Ty(),5);
        DerivedTypes.ArrayType tmp=DerivedTypes.ArrayType.get(tmp1,5);
        DerivedTypes.ArrayType tmp2=DerivedTypes.ArrayType.get(tmp1,5);

        ArrayList<Type> param_memset=new ArrayList<>();
        param_memset.add(DerivedTypes.PointerType.get(Type.getInt32Ty()));
        param_memset.add(Type.getInt32Ty());
        param_memset.add(Type.getInt32Ty());
        DerivedTypes.FunctionType FT1= DerivedTypes.FunctionType.get(Type.getInt32Ty(),param_memset);
        DerivedTypes.FunctionType FT2= DerivedTypes.FunctionType.get(Type.getInt1Ty(),param_memset);
        DerivedTypes.FunctionType FT3= DerivedTypes.FunctionType.get(Type.getFloatTy(),param_memset);
        System.out.println(FT1.equals(FT2));
        System.out.println(FT1.equals(FT3));

        DerivedTypes.PointerType PT1= DerivedTypes.PointerType.get(Type.getInt32Ty());
        DerivedTypes.PointerType PT2= DerivedTypes.PointerType.get(Type.getInt1Ty());
        DerivedTypes.PointerType PT3= DerivedTypes.PointerType.get(Type.getInt32Ty());
        System.out.println(PT1.equals(PT2));
        System.out.println(PT1.equals(PT3));
    }
}
