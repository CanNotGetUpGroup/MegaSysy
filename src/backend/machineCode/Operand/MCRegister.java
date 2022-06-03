package backend.machineCode.Operand;

public class MCRegister extends Register{

    private int id;
    private RegName name;

    public enum RegName{
        r0,
        r1,
        r2,
        r3,
        r4,
        r5,
        r6,
        r7,
        r8,
        r9,
        r10,
        r11,
        IP,
        SP,
        LR,
        PC,
        CPSR
    }

    public MCRegister(RegName reg){
        super(Type.MACHINE);
        this.name = reg;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
