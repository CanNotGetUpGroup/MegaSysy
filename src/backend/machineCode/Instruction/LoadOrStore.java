package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;

public class LoadOrStore extends MachineInstruction {
    public enum Type{
        LOAD,
        STORE
    }
    private final Type type;

    public LoadOrStore(MachineBasicBlock parent, Type type){
        super(parent);
        this.type = type;
    }

    public Type getType(){
        return type;
    }


}
