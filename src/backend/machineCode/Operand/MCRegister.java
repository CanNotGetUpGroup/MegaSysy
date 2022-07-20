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

    public static RegName idTORegName(int id){
        return switch (id){
            case 0 -> RegName.r0;
            case 1 -> RegName.r1;
            case 11 -> RegName.r11;
            case 12 -> RegName.IP;
            case 13 -> RegName.SP;
            case 14 -> RegName.LR;
            case 15 -> RegName.PC;
            case 16 -> RegName.CPSR;
            case 2 -> RegName.r2;
            case 3 -> RegName.r3;
            case 4 -> RegName.r4;
            case 5 -> RegName.r5;
            case 6 -> RegName.r6;
            case 7 -> RegName.r7;
            case 8 -> RegName.r8;
            case 9 -> RegName.r9;
            default -> null;
        };
    }

    public MCRegister(RegName reg){
        super(Type.MACHINE);
        this.name = reg;
    }

    public RegName getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
