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

    public enum Ops {
        //Branch
        BL, B,
        //Unary

        //Binary
        Add, Sub, Rsb, Mul, Div, Mod,
        //Memory
        LDR, STR,
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

    public MachineInstruction(MachineBasicBlock parent) {
        this.parent = parent;
        instNode = new IListNode<>(this, parent.getInstList());
        //插入到parent末尾
//        instNode.insertIntoListEnd(this.parent.getInstList());
    }

    public Register getDest() {
        return null;
    }

    public void setDest(Register dest) {

    }


    public MCOperand getOp1() {
        return null;
    }

    abstract public void setOp1(MCOperand op) ;

    public MCOperand getOp2() {
        return null;
    }

    abstract public void setOp2(MCOperand op);

}
