package backend;

import backend.machineCode.MachineDataBlock;
import backend.machineCode.MachineFunction;
import backend.pass.*;
import frontend.SysyLexer;
import frontend.SysyParser;
import frontend.Visitor;
import ir.DerivedTypes;
import ir.Function;
import ir.GlobalVariable;
import ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import pass.PassManager;
import util.IList;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CodeGenManager {

    private static Module module;
    private static CodeGenManager codeGenManager;

    //        private final ArrayList<GlobalVariable> globalVariables;
    private ArrayList<MachineDataBlock> dataBlockArrayList;
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

    private void halfRun1() {
        var selector = new InstructionSelector(module);
        selector.run();
        this.funcList = selector.getFuncList();
        this.dataBlockArrayList = selector.getGlobalDataList();

        var phiEliminate = new PhiElimination(funcList);
        phiEliminate.run();
    }

    private void halfRun2() {
        var allocator = new GraphColor(funcList);
        allocator.run();
        var clean = new Clean(funcList);
//        clean.run();
    }

    private void halfRun22() {
        var allocator = new RegAllocator(funcList);
        allocator.run();
        var clean = new Clean(funcList);
//        clean.run();
    }

    public void run() {
        var selector = new InstructionSelector(module);
        selector.run();
        this.funcList = selector.getFuncList();
        this.dataBlockArrayList = selector.getGlobalDataList();

        var phiEliminate = new PhiElimination(funcList);
        phiEliminate.run();

        var allocator = new RegAllocator(funcList);
        allocator.run();
    }

    public void performanceRun() {
        var selector = new InstructionSelector(module);
        selector.run();
        this.funcList = selector.getFuncList();
        this.dataBlockArrayList = selector.getGlobalDataList();

        var phiEliminate = new PhiElimination(funcList);
        phiEliminate.run();

        var allocator = new GraphColor(funcList);
        allocator.run();
    }



    public String toArm() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t.data\n");
        for (var block : dataBlockArrayList) {
            sb.append(block);
        }

        sb.append("\t.text\n");
        for (var func : funcList) {
            sb.append(func.toString());
        }

        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        CharStream inputStream = CharStreams.fromFileName("/Users/luxia/code/MegaSysy/src/backend/test/0in.txt"); // 获取输入流
        FileWriter fw1 = new FileWriter("/Users/luxia/code/MegaSysy/src/backend/test/1ir_out.txt");
        FileWriter fw2 = new FileWriter("/Users/luxia/code/MegaSysy/src/backend/test/2lir_out.txt");
        FileWriter fw3 = new FileWriter("/Users/luxia/code/MegaSysy/src/backend/test/3arm_out.txt");
        PrintWriter pw1 = new PrintWriter(fw1);
        PrintWriter pw2 = new PrintWriter(fw2);
        PrintWriter pw3 = new PrintWriter(fw3);

        SysyLexer lexer = new SysyLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer); // 词法分析获取 token 流
        Visitor visitor = new Visitor();
        SysyParser parser = new SysyParser(tokenStream);
        ParseTree tree = parser.program(); // 获取语法树的根节点
        visitor.visit(tree);
        Module module = Module.getInstance();
        module.rename();

        if (true) {
            //TODO：优化掉undef
            PassManager.ignoreUndef = false;
            PassManager.initialization();
            PassManager.initializationMC();
        }
        PassManager.run(module);

        pw1.println(module.toLL());
        pw1.flush();

        // back-end
        var mc = CodeGenManager.getInstance();
        mc.loadModule(module);
        mc.halfRun1();

        pw2.println(mc.toArm());
        pw2.flush();

        mc.halfRun22();

        pw3.println(mc.toArm());
        pw3.flush();
    }
}
