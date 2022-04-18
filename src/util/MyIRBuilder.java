package util;

import ir.*;
import ir.Constants.*;
import ir.instructions.CmpInst.*;
import ir.instructions.Instructions.*;

public class MyIRBuilder {
    private static final MyIRBuilder myIRBuilder=new MyIRBuilder();
    public BasicBlock BB;
    private IListNode<Instruction,BasicBlock> InsertPt;
    private MyContext context = MyContext.getInstance();

    public static MyIRBuilder getInstance(){return myIRBuilder;}

    public void InsertHelper(Instruction I, String Name, BasicBlock basicBlock, IListNode<Instruction,BasicBlock> insertPt) {
        if(basicBlock!=null) {
            //插入到基本块指定位置
            insertPt.insertBefore(I.getInstNode());
        }
        I.setName(Name);
    }

    public Instruction insert(Instruction I) {
        InsertHelper(I,"",BB,InsertPt);
        return I;
    }

    public Instruction insert(Instruction I,String name) {
        InsertHelper(I,name,BB,InsertPt);
        return I;
    }

    public void setInsertPoint(BasicBlock b){
        BB=b;
        InsertPt=b.getInstList().getTail();
    }

    public void setInsertPoint(Instruction I){
        BB=I.getParent();
        InsertPt=I.getInstNode();
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
        return insert(new ICmpInst(name,P,LHS,RHS),name);
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
