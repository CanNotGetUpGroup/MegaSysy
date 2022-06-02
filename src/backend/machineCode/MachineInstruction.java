package backend.machineCode;

import ir.BasicBlock;
import ir.Instruction;
import ir.Type;
import util.IListNode;
import util.MyIRBuilder;

public class MachineInstruction {
    private MachineBasicBlock Parent;
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
        return Parent;
    }

    public void setParent(MachineBasicBlock parent) {
        Parent = parent;
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

    public void pushBacktoInstList(){
        this.getInstNode().insertIntoListEnd(this.getParent().getInstList());
    }


}
