package pass.test;

import frontend.SysyLexer;
import frontend.SysyParser;
import frontend.Visitor;
import ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.FunctionPass;
import pass.PassManager;
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
        initModule();
        FileWriter fw=new FileWriter("src/pass/test/output.txt");
        PrintWriter pw=new PrintWriter(fw);

        PassManager.run(module);
        pw.println(module.toLL());
        pw.flush();
    }

    /**
     * 单独测试某pass
     */
    void runOnePassExample() throws IOException {
        //根据frontend中的input.txt初始化Module
        tmpTest.initModule();

        FunctionPass mem2reg=new Mem2Reg();
        mem2reg.runOnModule(module);

        //查看更改后的pass
        module.rename();
        System.out.println(module.toLL());
    }

    public static void initModule() throws IOException {
        CharStream inputStream = CharStreams.fromFileName("src/pass/test/input.txt"); // 获取输入流
        FileWriter fw=new FileWriter("src/pass/test/origin.txt");
        PrintWriter pw=new PrintWriter(fw);

        SysyLexer lexer = new SysyLexer(inputStream);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer); // 词法分析获取 token 流
        Visitor visitor=new Visitor();
        SysyParser parser = new SysyParser(tokenStream);
        ParseTree tree = parser.program(); // 获取语法树的根节点
        visitor.visit(tree);
        Module module = Module.getInstance();
        module.rename();

        pw.println(module.toLL());
        pw.flush();
    }
}
