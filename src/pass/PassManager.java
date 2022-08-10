package pass;

import backend.CodeGenManager;
import ir.Function;
import ir.Module;
import pass.passes.*;

import java.util.ArrayList;

public class PassManager {
    public static ArrayList<Pass> passes = new ArrayList<>();
    public static ArrayList<MCPass> MCPasses = new ArrayList<>();
    // 官方测例可以保证不会出现undef的情况，因此某些情况下可以激进地忽略掉undef（可能无法通过中端测试）
    public static boolean ignoreUndef = false;
    public static boolean eliminatePreHeader = false;
    public static boolean aggressive=false;

    /**
     * 初始化，在此处按照顺序添加IR pass
     */
    public static void initialization() {
        GVNGCM.GCMOpen=true;
//        eliminatePreHeader=true;//关闭LICM
        passes.add(new InterproceduralAnalysis());
        passes.add(new DeadCodeEmit());
        passes.add(new Mem2Reg());
        passes.add(new GVNGCM(aggressive));// Mem2Reg处理掉了所有local alloca
        passes.add(new LoopInfoUpdate()); // 计算循环信息
        passes.add(new LICM());// 循环不变量外提

        passes.add(new FuncInline());
        passes.add(new GlobalVariableOpt());// FuncInline为其创造更多机会
        passes.add(new Mem2Reg());// 处理掉新产生的alloca
        passes.add(new FuncInline());// 可能还有
        passes.add(new InterproceduralAnalysis());
        passes.add(new LoopInfoUpdate());
        passes.add(new LICM());// 循环不变量外提
        passes.add(new SimplifyCFG(eliminatePreHeader));

        eliminatePreHeader=true;//完成了循环优化，删掉preHeader
        aggressive=true;
//        aggressive=false;
        passes.add(new InterproceduralAnalysis());
        passes.add(new GVNGCM(aggressive));
        passes.add(new DeadCodeEmit());
        passes.add(new SimplifyCFG(eliminatePreHeader));
        passes.add(new EliminateAlloca());//由于GVN需要使用alloca，因此最后再删除
    }

    /**
     * 初始化，在此处按照顺序添加MC pass
     */
    public static void initializationMC() {
        // MC Pass
        // MCPasses.add(new Hello());
    }

    /**
     * 运行所有IR pass
     */
    public static void run(Module M) {
        for (Pass p : passes) {
            // System.out.println("IR Pass:"+p.getName());
            p.runOnModule(M);
            M.rename();
        }
    }

    /**
     * 运行所有MC pass
     */
    public static void runMC(CodeGenManager CGM) {
        for (MCPass mp : MCPasses) {
            System.out.println("MC Pass:" + mp.getName());
            mp.runOnCodeGen(CGM);
        }
    }
}
