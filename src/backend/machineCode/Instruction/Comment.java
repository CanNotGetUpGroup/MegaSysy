package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;

public class Comment extends MachineInstruction {

    public String getStr() {
        return str;
    }

    String str;

    public Comment(MachineBasicBlock parent, String str) {
        super(parent);
        this.str = str;
    }

    public Comment(MachineBasicBlock parent, Comment comment) {
        super(parent, comment);
        this.str = comment.getStr();
    }

    @Override
    public String toString() {
        return "@ " + str;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        return this;
    }
}
