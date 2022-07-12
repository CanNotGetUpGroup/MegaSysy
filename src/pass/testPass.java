package pass;

import frontend.SysyLexer;
import frontend.SysyParser;
import frontend.Visitor;
import ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.passes.Mem2Reg;
import frontend.test.tmpTest;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class testPass {
    private static final Module module=Module.getInstance();

    /**
     * 运行PassManager
     */
    public static void main(String[] args) throws IOException {
        //根据frontend中的input.txt初始化Module
        tmpTest.initModule();
        PassManager.run(module);
    }

    /**
     * 单独测试某pass
     */
    void runOnePassExample() throws IOException {
        //根据frontend中的input.txt初始化Module
        tmpTest.initModule();

        FunctionPass mem2reg=new Mem2Reg();
        mem2reg.runOnModule(module);
        module.rename();
        System.out.println(module.toLL());
    }
}
