package pass.test;

import ir.Module;
import pass.PassManager;
import pass.passes.LCSSA;
import pass.passes.Mem2Reg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class testLCSSA {
    private static final Module module=Module.getInstance();
    public static void main(String[] args) throws IOException {
        //根据frontend中的input.txt初始化Module
        testPass.initModule();
        FileWriter fw=new FileWriter("src/pass/test/output.txt");
        PrintWriter pw=new PrintWriter(fw);

        Mem2Reg mem2Reg=new Mem2Reg();
        mem2Reg.runOnModule(module);
        LCSSA lcssa=new LCSSA();
        lcssa.runOnModule(module);

        pw.println(module.toLL());
        pw.flush();
    }
}
