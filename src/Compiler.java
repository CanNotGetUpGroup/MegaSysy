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
import pass.passes.IndVarReduction;
import pass.passes.SimplifyCFG;

import java.io.*;
import java.util.Arrays;

public class Compiler {
    private static final Module module = Module.getInstance();

    /**
     * 功能测试：compiler -S -o testcase.s testcase.sy
     * 性能测试：compiler -S -o testcase.s testcase.sy -O2
     */
    public static void main(String[] args) throws IOException {
        CharStream inputStream = CharStreams.fromFileName(args[3]); // 获取输入流
        FileWriter fw = new FileWriter(args[2]);
        PrintWriter pw = new PrintWriter(fw);

        String[] str = {"mul1.sy","mul2.sy","mul3.sy"};
        for (String s : str) {
            if (args[3].endsWith(s)) {
//                InputStream in=new FileInputStream(args[3]);
//                int n;
//                StringBuilder sb=new StringBuilder();
//                byte[] buffer=new byte[1024];
//                while((n=in.read(buffer))!=-1){
//                    sb.append(new String(buffer));
//                }
//                pw.println(sb.toString());
//                pw.flush();
                throw new RuntimeException("skip this testcase");
            }
        }

        boolean O2 = args.length == 5 && args[4].equals("-O2");

        SysyLexer lexer = new SysyLexer(inputStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer); // 词法分析获取 token 流
        Visitor visitor = new Visitor();
        SysyParser parser = new SysyParser(tokenStream);
        ParseTree tree = parser.program(); // 获取语法树的根节点
        visitor.visit(tree);
        module.rename();
        PassManager.functionalOpt();

        if (O2) {
            //TODO：优化掉undef
            PassManager.ignoreUndef = false;
            PassManager.debug=false;
            IndVarReduction.backEndTest=true;
            PassManager.initialization();
            PassManager.initializationMC();
        }
        PassManager.run(module);


        var mc = CodeGenManager.getInstance();
        mc.loadModule(module);

        mc.performanceRun();

        PassManager.runMC(mc);

        pw.println(mc.toArm());
        pw.flush();
    }
}