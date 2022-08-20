package pass;

import analysis.MemorySSA;
import backend.CodeGenManager;
import ir.Function;
import ir.Module;
import pass.passes.*;
import backend.pass.*;

import java.util.ArrayList;

public class PassManager {
    public static ArrayList<Pass> passes = new ArrayList<>();
    public static ArrayList<MCPass> MCPasses = new ArrayList<>();
    // 官方测例可以保证不会出现undef的情况，因此某些情况下可以激进地忽略掉undef（可能无法通过中端测试）
    public static boolean ignoreUndef = false;
    public static boolean eliminatePreHeader = false;
    public static boolean aggressive = false;
    public static boolean debug = true;
    public static boolean openArraySSA=false;

    /**
     * 初始化，在此处按照顺序添加IR pass
     */
    public static void initialization() {
        passes.clear();
        GVNGCM.GCMOpen = true;
        MemorySSA.arraySSA=openArraySSA;
        // eliminatePreHeader=true;//关闭LICM
        if(!openArraySSA){//不开ArraySSA，直接消除掉DimInfo}
            passes.add(new EliminateDimInfo());
        }
        passes.add(new AddCondPreBlock());
        passes.add(new SimplifyCFG(eliminatePreHeader));
        passes.add(new Mem2Reg());// 消除掉local int(or float)的alloca，确保DCE消除store的正确
        // 只分析一次，函数内联后可能会改变side effect(没有side effect的函数内联进了side effect函数)
        passes.add(new InterproceduralAnalysis());
        passes.add(new DeadCodeEmit());
        passes.add(new GlobalVariableOpt());
        passes.add(new GVNGCM(aggressive));// Mem2Reg处理掉了所有local alloca

        passes.add(new LCSSA());
        passes.add(new LoopInfoUpdate()); // 计算循环信息
        passes.add(new IndVarReduction());
        passes.add(new LICM());// 循环不变量外提
        passes.add(new InterProceduralDCE());
        passes.add(new GVNGCM(aggressive));
//        passes.add(new LoopRedundant());

        passes.add(new FuncInline());
        passes.add(new GlobalVariableOpt());// FuncInline为其创造更多机会
        passes.add(new Mem2Reg());// 处理掉新产生的alloca
        passes.add(new FuncInline());// 可能还有
        passes.add(new InterProceduralDCE());
        passes.add(new LoopInfoUpdate());
        passes.add(new LICM());// 循环不变量外提
//        passes.add(new LCSSA());
//        passes.add(new LoopFusion());
        passes.add(new SimplifyCFG(eliminatePreHeader));

        passes.add(new GVNGCM(aggressive));
//        passes.add(new LoopRedundant());
        passes.add(new DeadCodeEmit());
        passes.add(new SimplifyCFG(eliminatePreHeader));
        passes.add(new LoopUnroll(true));// 常量循环消除
        passes.add(new LocalArrayPromote());
        passes.add(new InterProceduralDCE());
        passes.add(new GlobalVariableOpt());
//         passes.add(new LoopUnroll(false));//还存在bug，开了也不知道能不能快，干脆不开了
//         passes.add(new LoopUnroll(false));
//        passes.add(new LoopRedundant());

        aggressive = true;// 激进的GVN，消除掉数组参数的alloca，并关闭ArraySSA
        eliminatePreHeader = true;// 完成了循环优化，删掉preHeader
        passes.add(new SimplifyCFG(eliminatePreHeader));
        passes.add(new GVNGCM(aggressive));
        passes.add(new EliminateDimInfo());//消除掉DimInfo可能对GVNGCM造成的影响
        passes.add(new GVNGCM(aggressive));
        passes.add(new EliminateAlloca());// 由于GVN需要使用alloca，因此最后再删除
        if (debug) {
            passes.add(new VerifyFunction());
        }
    }

    public static void functionalOpt() {
        passes.add(new Mem2Reg());// 消除掉local int(or float)的alloca，确保DCE消除store的正确
        passes.add(new InterproceduralAnalysis());
        passes.add(new DeadCodeEmit());
        passes.add(new GlobalVariableOpt());
        passes.add(new GVNGCM(true));
    }

    /**
     * 初始化，在此处按照顺序添加MC pass
     */
    public static void initializationMC() {
        // MC Pass
        // MCPasses.add(new Hello());
        MCPasses.add(new MergeBlock());
        MCPasses.add(new PeepHole());
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
            // System.out.println("MC Pass:" + mp.getName());
            mp.runOnCodeGen(CGM);
        }
    }
}
