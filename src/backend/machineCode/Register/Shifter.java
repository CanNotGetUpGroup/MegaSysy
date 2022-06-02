package backend.machineCode.Register;

import backend.machineCode.Instruction.Shift;

public class Shifter {
    private Shift shifter;

    @Override
    public String toString() {
        return shifter.getOp1().toString() + " " + shifter.getType().toString() + " " + shifter.getSh().toString();
    }
}
