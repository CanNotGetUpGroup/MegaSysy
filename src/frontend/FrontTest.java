package frontend;

import ir.Argument;
import ir.Function;
import ir.GlobalVariable;
import ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import util.IList;
import util.IListNode;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class FrontTest {
    public static void main(String[] args) throws IOException {
        CharStream inputStream = CharStreams.fromFileName("src/frontend/input.txt"); // 获取输入流
        FileWriter fw=new FileWriter("src/frontend/output.txt");
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
