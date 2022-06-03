package backend;

import backend.machineCode.MachineFunction;
import backend.pass.InstructionSelector;
import frontend.SysyLexer;
import frontend.SysyParser;
import frontend.Visitor;
import ir.Function;
import ir.GlobalVariable;
import ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import util.IList;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CodeGenManager {

    private static Module module;
    private static CodeGenManager codeGenManager;

    //    private final ArrayList<GlobalVariable> globalVariables;
    private ArrayList<MachineFunction> funcList;

    private CodeGenManager() {
        funcList = new ArrayList<>();
    }

    public static CodeGenManager getInstance() {
        if (codeGenManager == null) {
            codeGenManager = new CodeGenManager();
        }
        return codeGenManager;
    }

    public void loadModule(Module module) {
        CodeGenManager.module = module;
    }

    public void run() {
        var selector = new InstructionSelector(module);
        selector.run();
        this.funcList = selector.getFuncList();
    }

    public String toArm() {
        StringBuilder sb = new StringBuilder();

        sb.append(".text\n");
        for (var func : funcList) {
            sb.append(func.toString());
        }

        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        CharStream inputStream = CharStreams.fromFileName("/Users/luxia/code/MegaSysy/src/backend/test/in.txt"); // 获取输入流
        FileWriter fw = new FileWriter("/Users/luxia/code/MegaSysy/src/backend/test/out.txt");
        PrintWriter pw = new PrintWriter(fw);

        SysyLexer lexer = new SysyLexer(inputStream);

        CommonTokenStream tokenStream = new CommonTokenStream(lexer); // 词法分析获取 token 流
        Visitor visitor = new Visitor();
        SysyParser parser = new SysyParser(tokenStream);
        ParseTree tree = parser.program(); // 获取语法树的根节点
        visitor.visit(tree);
        Module module = Module.getInstance();
        module.rename();

        var mc = CodeGenManager.getInstance();
        mc.loadModule(module);
        mc.run();

//        pw.println(module.toLL());
        pw.println(mc.toArm());
        pw.flush();
    }
}
