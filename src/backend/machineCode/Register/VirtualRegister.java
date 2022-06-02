package backend.machineCode.Register;

public class VirtualRegister {
    private static int c = 0;
    private static int counter(){
        return c++;
    }

    private String name;



    public VirtualRegister(){
        name = Integer.toString(counter());
    }

}
