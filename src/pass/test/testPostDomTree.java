package pass.test;

import analysis.DominatorTree;
import analysis.PostDominatorTree;
import ir.BasicBlock;
import ir.Function;
import ir.Module;
import pass.passes.AddCondPreBlock;
import pass.passes.LCSSA;
import pass.passes.Mem2Reg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class testPostDomTree {
    private static final Module module=Module.getInstance();
    public static void main(String[] args) throws IOException {
        //根据frontend中的input.txt初始化Module
        testPass.initModule();
        FileWriter fw=new FileWriter("src/pass/test/output.txt");
        PrintWriter pw=new PrintWriter(fw);

        AddCondPreBlock addCondPreBlock=new AddCondPreBlock();
        addCondPreBlock.runOnModule(module);
        Function firstF=module.getFuncList().getFirst().getVal();
        while(!firstF.isDefined()){
            firstF=firstF.getFuncNode().getNext().getVal();
        }
        DominatorTree DT=new DominatorTree(firstF);
        PostDominatorTree PDT=new PostDominatorTree(firstF);
        BasicBlock entry=firstF.getEntryBB();
        BasicBlock exit=PDT.PostRoot.BB;
        System.out.println(DT.dominates(entry,exit));
        System.out.println(PDT.dominates(exit,entry));

        pw.println(module.toLL());
        pw.flush();
    }
}
