package pass;

import backend.CodeGenManager;
import ir.Function;
import ir.Module;
import pass.passes.FuncInline;
import pass.passes.Mem2Reg;
import pass.passes.SimplifyCFG;
import pass.passes.InterproceduralAnalysis;
import pass.passes.DeadCodeEmit;

import java.util.ArrayList;

public class PassManager {
    public static ArrayList<Pass> passes=new ArrayList<>();
    public static ArrayList<MCPass> MCPasses=new ArrayList<>();

    /**
     * 初始化，在此处按照顺序添加IR pass
     */
    public static void initialization(){
        passes.add(new InterproceduralAnalysis());
        passes.add(new DeadCodeEmit());
        passes.add(new FuncInline());

        passes.add(new Mem2Reg());
        passes.add(new InterproceduralAnalysis());
        passes.add(new DeadCodeEmit());
        passes.add(new SimplifyCFG());
    }

    /**
     * 初始化，在此处按照顺序添加MC pass
     */
    public static void initializationMC(){
        //MC Pass
//        MCPasses.add(new Hello());
    }

    /**
     * 运行所有IR pass
     */
    public static void run(Module M){
        for(Pass p:passes){
            System.out.println("IR Pass:"+p.getName());
            p.runOnModule(M);
        }
    }

    /**
     * 运行所有MC pass
     */
    public static void runMC(CodeGenManager CGM){
        for(MCPass mp:MCPasses){
            System.out.println("MC Pass:"+mp.getName());
            mp.runOnCodeGen(CGM);
        }
    }

    /**
     * example
     */
    public static class Hello extends FunctionPass{
        @Override
        public void runOnFunction(Function F) {
            System.out.println("hello!"+F.getName());
        }

        @Override
        public String getName() {
            return "Hello";
        }
    }
}
