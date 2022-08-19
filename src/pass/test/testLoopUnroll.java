package pass.test;

import ir.Module;
import pass.passes.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class testLoopUnroll {
    private static final Module module=Module.getInstance();
    public static void main(String[] args) throws IOException {
        //根据frontend中的input.txt初始化Module
        testPass.initModule();
        FileWriter fw=new FileWriter("src/pass/test/output.txt");
        PrintWriter pw=new PrintWriter(fw);

        AddCondPreBlock addCondPreBlock=new AddCondPreBlock();
        Mem2Reg mem2Reg=new Mem2Reg();
        LCSSA lcssa=new LCSSA();
        GVNGCM gvngcm=new GVNGCM(true);
        SimplifyCFG simplifyCFG=new SimplifyCFG(false);
        LoopUnroll loopUnroll=new LoopUnroll(true);
        InterproceduralAnalysis interproceduralAnalysis=new InterproceduralAnalysis();

        addCondPreBlock.runOnModule(module);
        interproceduralAnalysis.runOnModule(module);
        mem2Reg.runOnModule(module);
        simplifyCFG.runOnModule(module);
        gvngcm.runOnModule(module);
        lcssa.runOnModule(module);
        loopUnroll.runOnModule(module);
        gvngcm.runOnModule(module);
        new LoopUnroll(true).runOnModule(module);

//        new SimplifyCFG(false).runOnModule(module);
//        new LCSSA().runOnModule(module);
//        new LoopUnroll().runOnModule(module);
//        new GVNGCM(true).runOnModule(module);

//        new SimplifyCFG(false).runOnModule(module);
//        new LCSSA().runOnModule(module);
//        new LoopUnroll().runOnModule(module);
//        new GVNGCM(true).runOnModule(module);
//
//        new SimplifyCFG(false).runOnModule(module);
//        new LCSSA().runOnModule(module);
//        new LoopUnroll().runOnModule(module);
//        new GVNGCM(true).runOnModule(module);

        pw.println(module.toLL());
        pw.flush();
    }
}
