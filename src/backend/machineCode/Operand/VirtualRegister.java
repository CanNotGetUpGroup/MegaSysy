package backend.machineCode.Operand;

import java.util.Objects;

import static backend.machineCode.Operand.Register.Type.VIRTUAL;

public class VirtualRegister extends Register {
    private static int c = 0;

    public int getColorId() {
        return colorId;
    }

    public void setColorId(int colorId) {
        this.colorId = colorId;
    }

    private int colorId = -1;

    private static int counter() {
        return c++;
    }

    public String getName() {
        return name;
    }

    private final String name;

    public VirtualRegister() {
        super(VIRTUAL);
        this.id = counter();
        name = "v" + (isFloat() ? "f" : "i") + id;
    }

    public VirtualRegister(Content content) {
        super(VIRTUAL, content);
        this.id = counter();
        name = "v" + (isFloat() ? "f" : "i") + id;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRegister) {
            var r = (MCRegister) obj;
            if (getColorId() == -1)
                return false;
            else
                return r.getId() == getColorId() && r.getContent() == getContent();
        }
        if (!(obj instanceof Register))
            return false;
            // throw new RuntimeException("can't compare" + obj + " " + this);

        var reg = (VirtualRegister) obj;
        return reg.getContent() == getContent() && Objects.equals(reg.getName(), getName());
    }

    @Override
    public String toString() {
        if (colorId != -1) {
            return (isFloat() ? "s" : "r") + colorId;
        }

        return name;
    }
}
