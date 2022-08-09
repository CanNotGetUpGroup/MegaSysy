package backend.machineCode.Operand;

import backend.machineCode.Instruction.Shift;

public class Shifter {
    private Shift.Type type;
    private int sh;

    public Shifter(Shift.Type type, int sh) {
        this.type = type;
        this.sh = sh;
    }

    @Override
    public String toString() {
        return type.toString() + " " + sh;
    }
}
