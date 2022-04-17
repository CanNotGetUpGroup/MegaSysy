package util;

import ir.*;
import ir.Constants.*;
import ir.instructions.CmpInst.*;
import ir.instructions.Instructions.*;

public class MyIRBuilder {
    public BasicBlock BB;
    public void InsertHelper(Instruction I, String Name, BasicBlock basicBlock) {
        if(basicBlock!=null) {
            //TODO:插入到基本块指定位置
        }
        I.setName(Name);
    }
    public Instruction Insert(Instruction I, String Name) {
        InsertHelper(I,Name,BB);
        return I;
    }

//    public GlobalValue createGlobalString() {
//
//    }
//
//    public ConstantInt getInt32() {
//
//    }
//
//    public AllocaInst createAlloca(Type ty) {
//
//    }
//
//    public Value createZExt(Value V, Type DestTy, String name) {
//
//    }

    public Value createICmp(Predicate P, Value LHS, Value RHS, String name) {
        //TODO: 常量折叠
        return Insert(new ICmpInst(name,P,LHS,RHS),name);
    }

    public Value createICmpEQ(Value LHS, Value RHS, String name) {
        return createICmp(Predicate.ICMP_EQ,LHS,RHS,name);
    }

    public Value createICmpNE(Value LHS, Value RHS, String name) {
        return createICmp(Predicate.ICMP_NE,LHS,RHS,name);
    }

    public Value createICmpSGE(Value LHS, Value RHS, String name) {
        return createICmp(Predicate.ICMP_SGE,LHS,RHS,name);
    }

    public Value createICmpSGT(Value LHS, Value RHS, String name) {
        return createICmp(Predicate.ICMP_SGT,LHS,RHS,name);
    }

    public Value createICmpSLE(Value LHS, Value RHS, String name) {
        return createICmp(Predicate.ICMP_SLE,LHS,RHS,name);
    }

    public Value createICmpSLT(Value LHS, Value RHS, String name) {
        return createICmp(Predicate.ICMP_SLT,LHS,RHS,name);
    }


}
