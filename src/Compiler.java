import backend.CodeGenManager;
import frontend.SysyLexer;
import frontend.SysyParser;
import frontend.Visitor;
import ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.PassManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Compiler {
    /**
     * 功能测试：compiler -S -o testcase.s testcase.sy
     * 性能测试：compiler -S -o testcase.s testcase.sy -O2
     */
    public static void main(String[] args) throws IOException {
        CharStream inputStream = CharStreams.fromFileName(args[3]); // 获取输入流
        FileWriter fw=new FileWriter(args[2]);
        PrintWriter pw=new PrintWriter(fw);
        boolean O2=false;
        if(args.length==5&&args[4].equals("-O2"))
            O2=true;

        SysyLexer lexer = new SysyLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer); // 词法分析获取 token 流
        Visitor visitor=new Visitor();
        SysyParser parser = new SysyParser(tokenStream);
        ParseTree tree = parser.program(); // 获取语法树的根节点
        visitor.visit(tree);
        Module module = Module.getInstance();
        module.rename();

        if(O2){
            PassManager.initialization();
            PassManager.initializationMC();
        }
        PassManager.run(module);

        var mc = CodeGenManager.getInstance();
        mc.loadModule(module);
        mc.run();
        PassManager.runMC(mc);

        pw.println(mc.toArm());
        pw.flush();
    }
}
