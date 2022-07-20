package backend.machineCode.Operand;

import backend.machineCode.Instruction.LoadOrStore;
import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;

public class ImmediateNumber extends MCOperand {
    public int getValue() {
        return value;
    }

    final int value;
    final boolean isLegalImm;

    public ImmediateNumber(int value) {
        super(Type.Imm);
        this.value = value;
        this.isLegalImm = isLegalImm(value);
    }

    static public boolean isLegalImm(int num) {
        if(num < 0) num = -num;
        for (int i = 0; i < 16; i++) {
            int head = (num & 3) << 30;
            num = (num >>> 2) | head;
            if ((num & ~0xff) == 0) return true;
        }
        return false;
    }



    static public MachineInstruction loadNum(MachineBasicBlock parent, Register reg, int num) {
        if (isLegalImm(num)) {
            return new Move(parent, reg, new ImmediateNumber(num));
        } else {
            return new LoadOrStore(parent, LoadOrStore.Type.LOAD, reg, new ImmediateNumber(num));
        }
    }

    @Override
    public String toString() {
        return (isLegalImm ? "#" : "=") + value;
    }

    public static void main(String[] args) {
        isLegalImm(-8);
        System.out.println(isLegalImm(-8));
    }
}
