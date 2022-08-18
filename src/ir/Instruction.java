package ir;

import util.CloneMap;
import util.IListNode;
import util.MyIRBuilder;

import java.util.ArrayList;

public abstract class Instruction extends User {
    private BasicBlock Parent;
    private IListNode<Instruction, BasicBlock> instNode;
    private Ops op;//指令类型

    public enum Ops {
        //Term
        Ret, Br,
        //Unary
        //Binary
        Add, FAdd, Sub, FSub, Mul, FMul, SDiv, FDiv, SRem, FRem,And,Or,Xor,
        //Memory
        Alloca, Load, Store, GetElementPtr, Fence,
        //Cast
        ZExt, SIToFP, FPToSI, BitCast,
        //Other
        ICmp, FCmp, Call, Select, PHI,
        //MemSSA
        MemDef,MemUse,MemPHI
    }

    public BasicBlock getParent() {
//        return Parent;
        if(instNode.getParent()==null) return null;
        return instNode.getParent().getVal();
    }

    public void setParent(BasicBlock parent) {
        Parent = parent;
    }

    public IListNode<Instruction, BasicBlock> getInstNode() {
        return instNode;
    }

    public void setInstNode(IListNode<Instruction, BasicBlock> instNode) {
        this.instNode = instNode;
    }

    public Ops getOp() {
        return op;
    }

    public void setOp(Ops op) {
        this.op = op;
    }

    public Instruction(Type type, Ops op, int numOperands) {
        super(type, numOperands);
        this.op = op;
        instNode = new IListNode<>(this, MyIRBuilder.getInstance().BB.getInstList());
    }

    public Instruction(Type type,Ops op){
        super(type,0);
        this.op=op;
    }

    public Instruction(Type type, Ops op, String name, int numOperands) {
        super(type, name, numOperands);
        this.op = op;
        instNode = new IListNode<>(this, MyIRBuilder.getInstance().BB.getInstList());
    }

    /**
     * @param InsertBefore 插入在这个指令之前
     */
    public Instruction(Type type, Ops op, int numOperands, Instruction InsertBefore) {
        super(type, numOperands);
        this.op = op;
        //插入在这个指令之前
        instNode=new IListNode<>(this,InsertBefore.getParent().getInstList());
        instNode.insertBefore(InsertBefore.getInstNode());
    }

    /**
     * @param type        类型
     * @param numOperands 参数数量
     * @param InsertAtEnd 插入在这个基本块的最后
     */
    public Instruction(Type type, Ops op, int numOperands, BasicBlock InsertAtEnd) {
        super(type, numOperands);
        this.op = op;
        //插入在这个基本块的最后
        instNode=new IListNode<>(this,InsertAtEnd.getInstList());
        instNode.insertIntoListEnd(InsertAtEnd.getInstList());
    }

    // x op (y op z) === (x op y) op z
    public static boolean isAssociative(Ops Opcode) {
        return switch (Opcode) {
            case Add, FAdd, Mul, FMul, And, Or -> true;
            default -> false;
        };
    }

    // (x op y) === (y op x)
    public static boolean isCommutative(Ops Opcode){
        return switch (Opcode) {
            case Add, FAdd, Mul, FMul, And, Or, Xor -> true;
            default -> false;
        };
    }

    public boolean isTerminator(){
        return switch (getOp()) {
            case Ret, Br -> true;
            default -> false;
        };
    }

    public static boolean isBinary(Ops op){
        return op.ordinal() >= Ops.Add.ordinal()
            && op.ordinal() <= Ops.Xor.ordinal();
    }

    public static boolean isCmp(Ops op){
        return op.equals(Ops.ICmp)||op.equals(Ops.FCmp);
    }

    /**
     * 从基本块中删除
     */
    public void remove(){
        instNode.remove();
        dropUsesAsValue();
        dropUsesAsUser();
    }

    public ArrayList<BasicBlock> getSuccessors(){
        return new ArrayList<>();
    }

    public int getSuccessorsNum(){
        return 0;
    }

    public BasicBlock getSuccessor(int idx){
        return null;
    }

    public void setSuccessor(int idx,BasicBlock BB){
    }

    public void replaceSuccessorWith(BasicBlock OldBB,BasicBlock newBB){
        for(int i=0;i<getSuccessorsNum();i++){
            if(getSuccessor(i)==OldBB){
                setSuccessor(i,newBB);
            }
        }
    }

    public Function getFunction(){
        if(getParent()==null) return null;
        return getParent().getParent();
    }


    /**
     * 浅拷贝，Operand不拷贝
     */
    public abstract Instruction shallowCopy();
}
