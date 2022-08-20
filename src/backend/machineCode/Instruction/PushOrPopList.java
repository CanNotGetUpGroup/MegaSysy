package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.List;

public class PushOrPopList extends MachineInstruction {

    public PushOrPopList(MachineBasicBlock parent, PushOrPop.Type type) {
        super(parent);
        this.regList = new ArrayList<>();
        this.type = type;
    }

    public PushOrPopList(MachineBasicBlock parent, PushOrPopList inst) {
        super(parent, inst);
        this.regList = inst.regList;
        this.type = inst.type;
    }

    ArrayList<Register> regList;

    public PushOrPop.Type getType() {
        return type;
    }

    PushOrPop.Type type;

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append(isForFloat() ? "v" : "").append(type.name()).append(condString()).append('\t').append(" { ");
        regList.forEach(op -> sb.append(op.toString()).append(", "));
        sb.replace(sb.length()-2, sb.length()-1, " }");
        return sb.toString();
    }

    public void AddReg(Register op) {
        regList.add(op);
    }

    public ArrayList<Register> getRegList() {
        return regList;
    }


    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        setForFloat(isForFloat, new ArrayList<>());
        return this;
    }

    @Override
    public ArrayList<Register> getUse() {
        if (type == PushOrPop.Type.Push) return new ArrayList<Register>(regList);
        return new ArrayList<Register>();
    }

    @Override
    public ArrayList<Register> getDef() {
        if (type == PushOrPop.Type.Pop) return new ArrayList<Register>(regList);
        return new ArrayList<Register>();
    }
}
