import ir.*;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;

import java.util.HashMap;

public class test {
    public static void main(String[] args) {
        MyContext context = MyContext.getInstance();
        Module module=new Module();
        context.getValSymTab().add(new HashMap<>());

        context.getGlobalValSymTab().put("a",new Function(new Type(),"a",module));
        Instruction I=new Instructions.AllocaInst(new Type(),"a",2);
    }
}
