package backend.machineCode.Operand;

import backend.machineCode.Instruction.LoadImm;
import backend.machineCode.Instruction.LoadOrStore;
import backend.machineCode.Instruction.Move;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import util.IListNode;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ImmediateNumber extends MCOperand {
    public int getValue() {
        return value;
    }

    public enum Type {
        Int,
        Float;
    }

    final int value;
    final boolean isLegalImm;
    final Type type;

    public ImmediateNumber(int value) {
        super(MCOperand.Type.Imm);
        this.value = value;
        this.type = Type.Int;
        this.isLegalImm = isLegalImm(value);
    }

    public ImmediateNumber(float value) {
        super(MCOperand.Type.Imm);
        this.value = Float.floatToIntBits(value);
        this.type = Type.Float;
        this.isLegalImm = false;
    }

    static public boolean isLegalImm(int num) {
        if (num < 0) num = -num;
        for (int i = 0; i < 16; i++) {
            int head = (num & 3) << 30;
            num = (num >>> 2) | head;
            if ((num & ~0xff) == 0) return true;
        }
        return false;
    }

    static public Register loadNum(MachineBasicBlock parent, Register reg, int num) {
        return loadNumInsertBefore(parent, reg, num, parent.getInstList().getTail());
    }

    static public Register loadNumInsertBefore(MachineBasicBlock parent, Register reg, int num, IListNode<MachineInstruction, MachineBasicBlock> node) {
        if (isLegalImm(num)) {
            new Move(parent, reg, new ImmediateNumber(num)).setForFloat(reg.isFloat(), new ArrayList<>(Arrays.asList("32"))).insertBefore(node);
        } else {
            if (!reg.isFloat()) {
                new LoadImm(parent, reg, num).insertBefore(node);
            } else {
                var temp = new VirtualRegister();
                new LoadImm(parent, temp, num).insertBefore(node);
                new Move(parent, reg, temp).insertBefore(node);
            }

        }
        return reg;
    }

    static public MCOperand getLegalOperand(MachineBasicBlock parent, int value) {
        if (isLegalImm(value)) {
            return new ImmediateNumber(value);
        } else {
            Register reg = new VirtualRegister();
            new LoadImm(parent, reg, value).pushBacktoInstList();
            return reg;
        }
    }

    static public MCOperand getLegalOperandInsertBefore(IListNode<MachineInstruction, MachineBasicBlock> node, int value) {
        if (isLegalImm(value)) {
            return new ImmediateNumber(value);
        } else {
            Register reg = new VirtualRegister();
            new LoadImm(node.getParent().getVal(), reg, value).getInstNode().insertBefore(node);
            return reg;
        }
    }

    static public MCOperand getLegalOperand(MachineBasicBlock parent, int value, Register.Content type) {
        if (isLegalImm(value)) {
            return new ImmediateNumber(value);
        } else {
            Register reg = new VirtualRegister(type);
            new LoadImm(parent, reg, value).pushBacktoInstList();
            return reg;
        }
    }


    @Override
    public String toString() {
        return (isLegalImm ? "#" : "") + value;
    }

    public static void main(String[] args) {
        isLegalImm(-8);
        System.out.println(isLegalImm(-8));
    }
}
