package backend.machineCode.Operand;

public class VirtualRegister {
    private static int c = 0;
    private static int counter(){
        return c++;
    }

    private String name;



    public VirtualRegister(){
        name = "v" + Integer.toString(counter());
    }

    @Override
    public String toString() {
        return name;
    }
}
