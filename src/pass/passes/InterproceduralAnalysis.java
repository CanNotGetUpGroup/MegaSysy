package pass.passes;

import ir.*;
import ir.Module;
import pass.ModulePass;
import ir.instructions.Instructions.*;
import analysis.DominatorTree.TreeNode;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.*;

public class InterproceduralAnalysis extends ModulePass {

    public InterproceduralAnalysis() {
        super();
    }
    
    @Override
    public void runOnModule(Module M){
        for(Function F : M.getFuncList()) {
            F.getCalleeList().clear();
            F.getCallerList().clear();
            F.setSideEffect(true); // TODO: sideEffect判断
            // F.setSideEffect(!F.isDefined());
        }
        for(Function F : M.getFuncList()) {
            for(BasicBlock BB : F.getBbList()) {
                for(Instruction I : BB.getInstList()) {
                    switch(I.getOp()) {
                        // Call指令 更新caller callee List
                        case Call -> {
                            Function calleeFunction = ((CallInst) I).getCalledFunction();
                            F.getCalleeList().add(calleeFunction);
                            calleeFunction.getCallerList().add(F);
                        }
                        // case Store -> {

                        // }
                        // case Load -> {

                        // }
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "InterproceduralAnalysis";
    }
}
