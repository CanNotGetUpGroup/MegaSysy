package pass;

import ir.Function;
import ir.Module;
import pass.passes.FuncInline;
import pass.passes.Mem2Reg;

import java.util.ArrayList;

public class PassManager {
    public static ArrayList<Pass> passes;

    /**
     * 初始化，在此处按照顺序添加pass
     */
    public static void initialization(){
        passes=new ArrayList<>();

        passes.add(new Hello());

//        passes.add(new Mem2Reg());
//        passes.add(new FuncInline());
    }

    /**
     * 运行所有pass
     */
    public static void run(Module M){
        initialization();

        for(Pass p:passes){
            System.out.println("Pass Info:");
            System.out.println("Pass Name:"+p.getName());
            System.out.println("Optimizing......\n");
            p.runOnModule(M);
        }

        finalization();
    }

    public static void finalization(){

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
        public void runOnModule(Module M) {
            super.runOnModule(M);
        }

        @Override
        public String getName() {
            return "Hello";
        }
    }
}
