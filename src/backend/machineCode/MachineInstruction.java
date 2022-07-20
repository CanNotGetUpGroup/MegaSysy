package backend.machineCode;

import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import ir.BasicBlock;
import ir.Instruction;
import ir.Type;
import util.IList;
import util.IListNode;
import util.MyIRBuilder;

public abstract class MachineInstruction {
    private MachineBasicBlock parent;
    private IListNode<MachineInstruction, MachineBasicBlock> instNode;
    private MachineInstruction.Ops op;//指令类型

    private String comment;

    public Condition getCond() {
        return cond;
    }

    public void setCond(Condition cond) {
        this.cond = cond;
    }

    private Condition cond;



    public enum Condition{
        EQ, ///> Equal
        NE, ///> Not Equal
        GE, ///> Signed greater than or equal
        LT, ///> Signed less than
        GT, ///> Signed greater than
        LE; ///> Signed less than or equal

        static public Condition getOpposite(Condition cond){
            return switch (cond){
                case EQ -> NE;
                case NE -> EQ;
                case GE -> LT;
                case LT -> GE;
                case GT -> LE;
                case LE -> GT;
            };
        }
    }

    public MachineInstruction setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getComment() {
        return comment;
    }

    private boolean isPrologue = false; // 开头

    public boolean isPrologue() {
        return isPrologue;
    }

    public void setPrologue(boolean prologue) {
        isPrologue = prologue;
    }

    public boolean isEpilogue() {
        return isEpilogue;
    }

    public void setEpilogue(boolean epilogue) {
        isEpilogue = epilogue;
    }

    private boolean isEpilogue = false; // 结尾

    public enum Ops {
        //Branch
        BL, B,
        //Unary

        //Binary
        Add, Sub, Rsb, Mul, Div, Mod,
        //Memory
        Ldr, STR, Push, Pop
        //Cast

        //Other

    }

    public MachineBasicBlock getParent() {
        return parent;
    }

    public void setParent(MachineBasicBlock parent) {
        this.parent = parent;
    }

    public IListNode<MachineInstruction, MachineBasicBlock> getInstNode() {
        return instNode;
    }

    public void setInstNode(IListNode<MachineInstruction, MachineBasicBlock> instNode) {
        this.instNode = instNode;
    }

    public MachineInstruction.Ops getOp() {
        return op;
    }

    public void setOp(MachineInstruction.Ops op) {
        this.op = op;
    }

    public void pushBacktoInstList() {
        this.getInstNode().insertIntoListEnd(this.getParent().getInstList());
    }

    public void pushtofront() {
        this.getInstNode().insertBefore(this.getParent().getInstList().getFirst());
    }


    public MachineInstruction(MachineBasicBlock parent) {
        this.parent = parent;
        instNode = new IListNode<>(this, parent.getInstList());
    }



    public Register getDest() {
        return null;
    }

    public void setDest(Register dest) {

    }


    public MCOperand getOp1() {
        return null;
    }

     public void setOp1(MCOperand op) {

     }

    public MCOperand getOp2() {
        return null;
    }

    public void setOp2(MCOperand op){
    }


    public static void main(String[] args) {

    }

}
