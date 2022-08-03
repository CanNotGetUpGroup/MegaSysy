package pass.test;

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

public class PassTest {
    public static void main(String[] args) throws IOException {
        CharStream inputStream = CharStreams.fromFileName(args[0]); // 获取输入流
        FileWriter fw=new FileWriter(args[1]);
        PrintWriter pw=new PrintWriter(fw);

        SysyLexer lexer = new SysyLexer(inputStream);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer); // 词法分析获取 token 流
        Visitor visitor=new Visitor();
        SysyParser parser = new SysyParser(tokenStream);
        ParseTree tree = parser.program(); // 获取语法树的根节点
        visitor.visit(tree);
        Module module = Module.getInstance();
        module.rename();

        //添加Pass
        PassManager.initialization();
        PassManager.run(module);

        pw.println(module.toLL());
        pw.flush();
    }
}
