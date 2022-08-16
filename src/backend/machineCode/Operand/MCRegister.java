package backend.machineCode.Operand;

public class MCRegister extends Register {

    public int getId() {
        return id;
    }
    public boolean isAllocated = false;

    private int id;
    private RegName name;

    public static int maxRegNum(Content type){
        if(type == Content.Int) return 12;
        else return 32;
    }


    private String strName;

    public enum RegName {
        r0(0),
        r1(1),
        r2(2),
        r3(3),
        r4(4),
        r5(5),
        r6(6),
        r7(7),
        r8(8),
        r9(9),
        r10(10),
        r11(11),
        IP(12),
        SP(13),
        LR(14),
        PC(15),
        CPSR(16),
        FPSCR(17),
        APSR_nzcv(18);
        private final int value;

        private RegName(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static RegName idTORegName(int id) {
        return switch (id) {
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

    public MCRegister(RegName reg) {
        super(Register.Type.MACHINE);
        this.name = reg;
        this.id = reg.getValue();
    }

    public MCRegister(Content type, int id) {
        super(Register.Type.MACHINE, type);
        this.id = id;
    }

    public RegName getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof VirtualRegister){
            var r = (VirtualRegister) obj;
            if(r.getColorId() == -1){
                return false;
            } else {
                return r.getContent() == this.getContent() && r.getColorId() == this.getId();
            }
        }
        if (!(obj instanceof Register))
            throw new RuntimeException("can't compare");
        if (!(obj instanceof MCRegister))
            return false;
        return getContent() == ((MCRegister) obj).getContent() && getId() == ((MCRegister) obj).getId();
    }

    @Override
    public String toString() {
        if (isInt()) {
            RegName reg = switch (id) {
                case 12 -> RegName.IP;
                case 13 -> RegName.SP;
                case 14 -> RegName.LR;
                case 15 -> RegName.PC;
                case 16 -> RegName.CPSR;
                case 17 -> RegName.FPSCR;
                case 18 -> RegName.APSR_nzcv;
                default -> null;
            };
            if (reg != null) return reg.name();
        }
        return (isInt() ? "r" : "s") + id;
    }

}
