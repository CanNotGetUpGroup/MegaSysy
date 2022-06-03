package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Move extends MachineInstruction {
    private Register dest;
    private MCOperand op;

    public Move(MachineBasicBlock parent, Register dest, MCOperand op){
        super(parent);
        this.dest = dest;
        this.op = op;
    }

    public String toString(){
        return "mov\t" + dest.toString() + ", " + op.toString();

    }
}
