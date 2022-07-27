package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;

public class Comment extends MachineInstruction {

    String str;
    public Comment(MachineBasicBlock parent, String str) {
        super(parent);
        this.str = str;
    }

    @Override
    public String toString() {
        return "@ " + str ;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        return this;
    }
}
