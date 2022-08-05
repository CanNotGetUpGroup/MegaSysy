package backend.machineCode.Operand;

import java.util.Objects;

import static backend.machineCode.Operand.Register.Type.VIRTUAL;

public class VirtualRegister extends Register {
    private static int c = 0;

    private static int counter() {
        return c++;
    }

    public String getName() {
        return name;
    }

    private String name;

    public VirtualRegister() {
        super(VIRTUAL);
        name = "v" + counter();
    }

    public VirtualRegister(Content content) {
        super(VIRTUAL, content);
        name = "v" + counter();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Register))
            throw new RuntimeException("can't compare");
        if(obj instanceof MCRegister)
            return false;
        var reg = (VirtualRegister) obj;
        return reg.getType() == getType() && Objects.equals(reg.getName(), reg.getName());
    }

    @Override
    public String toString() {
        return name;
    }
}
