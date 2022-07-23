package example;

import ir.*;
import ir.Module;
import ir.DerivedTypes.*;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;
import util.MyIRBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class test {
    public static void main(String[] args) {
        createIR();
//        Module module=Module.getInstance();
//        Function function=new Function(null,"test",module);
//        BasicBlock bb=new BasicBlock(function);
//        MyIRBuilder myIRBuilder=MyIRBuilder.getInstance();
//        myIRBuilder.setInsertPoint(bb);
//        bb.getInstList().pushBack(new Instructions.AllocaInst(Type.getInt1Ty()));
//        bb.getInstList().pushBack(new Instructions.AllocaInst(Type.getInt1Ty()));
//        for(var i:bb.getInstList()){
//            System.out.println(i);
//        }
//        //float hex test
//        String text = "0x1.921fb6p+1";
//        float number = Float.parseFloat(text);
//        System.out.println(number);

//        MyContext context = MyContext.getInstance();
//        Module module=Module.getInstance();
//        DerivedTypes.ArrayType tmp1=DerivedTypes.ArrayType.get(Type.getInt32Ty(),5);
//        DerivedTypes.ArrayType tmp=DerivedTypes.ArrayType.get(tmp1,5);
//        DerivedTypes.ArrayType tmp2=DerivedTypes.ArrayType.get(tmp1,5);
//
//        ArrayList<Type> param_memset=new ArrayList<>();
//        param_memset.add(DerivedTypes.PointerType.get(Type.getInt32Ty()));
//        param_memset.add(Type.getInt32Ty());
//        param_memset.add(Type.getInt32Ty());
//        DerivedTypes.FunctionType FT1= DerivedTypes.FunctionType.get(Type.getInt32Ty(),param_memset);
//        DerivedTypes.FunctionType FT2= DerivedTypes.FunctionType.get(Type.getInt1Ty(),param_memset);
//        DerivedTypes.FunctionType FT3= DerivedTypes.FunctionType.get(Type.getFloatTy(),param_memset);
//        System.out.println(FT1.equals(FT2));
//        System.out.println(FT1.equals(FT3));
//
//        DerivedTypes.PointerType PT1= DerivedTypes.PointerType.get(Type.getInt32Ty());
//        DerivedTypes.PointerType PT2= DerivedTypes.PointerType.get(Type.getInt1Ty());
//        DerivedTypes.PointerType PT3= DerivedTypes.PointerType.get(Type.getInt32Ty());
//        System.out.println(PT1.equals(PT2));
//        System.out.println(PT1.equals(PT3));
//
//        String text="0x7FFFFFFF";
//
//        int value=new BigInteger(text.substring(2),16).intValue();
//        System.out.println(value);
//
//        Value v= Constants.ConstantInt.const_0();

    }

    public static void createIR(){
        Module module=Module.getInstance();
        MyIRBuilder builder=MyIRBuilder.getInstance();

        FunctionType FT=FunctionType.get(Type.getInt32Ty(),new ArrayList<>(){{
            add(Type.getInt32Ty());
            add(Type.getInt32Ty());
        }});
        Function F=builder.createFunction(FT,"f",module);

        var m=new Argument(Type.getInt32Ty(),F,0);
        var n=new Argument(Type.getInt32Ty(),F,1);
        F.getArguments().add(m);
        F.getArguments().add(n);

        BasicBlock entry_F=builder.createBasicBlock("entry",F);
        F.setEntryBB(entry_F);
        builder.setInsertPoint(entry_F);

        //%3 = alloca i32     ; get n
        var alloca_n = builder.createAlloca(Type.getInt32Ty());
        //%4 = alloca i32     ; get m
        var alloca_m = builder.createAlloca(Type.getInt32Ty());
        //store i32 %0, i32* %4
        var store_m = builder.createStore(m,alloca_m);
        //store i32 %1, i32* %3
        var store_n = builder.createStore(n,alloca_n);
        //%5 = load i32, i32* %4
        var load_m = builder.createLoad(alloca_m);
        //%6 = load i32, i32* %3
        var load_n = builder.createLoad(alloca_n);
        //%7= icmp sgt i32 %5, %6     ; m>n
        var cmp_mn = builder.createCmp(CmpInst.Predicate.ICMP_SGT,load_m,load_n);
        //br i1 %7, label %8, label %9     ; judge m>n
        var br = builder.createCondBr(cmp_mn,null,null);

        //8:     ; if Stmt
        BasicBlock TrueBlock = builder.createBasicBlock(F);
        ((Instructions.BranchInst)br).setIfTrue(TrueBlock);
        builder.setInsertPoint(TrueBlock);
        //ret i32 0
        builder.createRet(Constants.ConstantInt.get(0));

        //9:     ; else Stmt
        BasicBlock FalseBlock = builder.createBasicBlock(F);
        ((Instructions.BranchInst)br).setIfFalse(FalseBlock);
        builder.setInsertPoint(FalseBlock);
        builder.createRet(Constants.ConstantInt.get(1));

        module.rename();
        System.out.println(module.toLL());

        //--------------------
        //遍历函数
        for(Function f:module.getFuncList()){
            //遍历基本块
            for(BasicBlock BB:f.getBbList()){
                //遍历指令
                for(Instruction I:BB.getInstList()){
                    I.remove();
                }
                //列表开头插入
                BB.getInstList().insertAtHead(new Instructions.AllocaInst(Type.getInt32Ty()));
                //列表末尾插入
                BB.getInstList().pushBack(new Instructions.StoreInst(m,alloca_n));
            }
        }
//        System.out.println(module.toLL());
    }
}
