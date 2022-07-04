package backend.machineCode.Operand;

import static backend.machineCode.Operand.Register.Type.VIRTUAL;

public class VirtualRegister extends Register{
    private static int c = 0;
    private static int counter(){
        return c++;
    }

    private String name;



    public VirtualRegister(){
        super(VIRTUAL);
        name = "v" + counter();
    }

    @Override
    public String toString() {
        return name;
    }
}
