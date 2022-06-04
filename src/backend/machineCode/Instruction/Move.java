package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Move extends MachineInstruction {
    private Register dest;
    private MCOperand op;

    public Move(MachineBasicBlock parent, MCOperand dest, MCOperand op){
        super(parent);
        assert dest instanceof Register;
        this.dest = (Register) dest;
        this.op = op;
    }

    public String toString(){
        return "mov\t" + dest.toString() + ", " + op.toString();

    }
}
