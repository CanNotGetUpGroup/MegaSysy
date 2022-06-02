package backend.machineCode.Instruction;

import backend.machineCode.MachineInstruction;
import backend.machineCode.Register.Register;

public class Move extends MachineInstruction {
    private Register dest;
    private MCOperand op;

    public Move(Register dest, MCOperand op){
        this.dest = dest;
        this.op = op;
    }

    public String toString(){
        return "mov\t" + dest.toString() + ", " + op.toString();

    }
}
