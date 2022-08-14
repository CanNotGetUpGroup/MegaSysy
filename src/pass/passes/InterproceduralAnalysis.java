package pass.passes;

import ir.*;
import ir.Module;
import ir.DerivedTypes.PointerType;
import pass.ModulePass;
import ir.instructions.Instructions.*;
import analysis.AliasAnalysis;
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
            // F.setSideEffect(true); // TODO: sideEffect判断
            F.setSideEffect(!F.isDefined());
            F.setUseGlobalVars(!F.isDefined());
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
                        case Store -> {
                            Value addr = I.getOperand(1);
                            if(addr instanceof AllocaInst && (((AllocaInst) addr).getAllocatedType().isInt32Ty()||((AllocaInst) addr).getAllocatedType().isFloatTy())){
                                continue;
                            }
                            Value pointer = AliasAnalysis.getPointerValue(addr);
                            if(!AliasAnalysis.isLocal(pointer)){
                                F.setSideEffect(true);
                                if(AliasAnalysis.isGlobal(pointer)){
                                    GlobalVariable gv = (GlobalVariable) pointer;
                                    if(((PointerType) gv.getType()).getElementType().isInt32Ty() || ((PointerType) gv.getType()).getElementType().isFloatTy()) {
                                        F.getStoreGlobalVars().add(gv);
                                    }
                                }
                            }
                        }
                        case Load -> {
                            Value addr = I.getOperand(0);
                            if(addr instanceof AllocaInst && (((AllocaInst) addr).getAllocatedType().isInt32Ty()||((AllocaInst) addr).getAllocatedType().isFloatTy())){
                                continue;
                            }
                            Value pointer = AliasAnalysis.getPointerValue(addr);
                            if(AliasAnalysis.isGlobal(pointer)){
                                F.setUseGlobalVars(true);
                                GlobalVariable gv = (GlobalVariable) pointer;
                                if(((PointerType) gv.getType()).getElementType().isInt32Ty() || ((PointerType) gv.getType()).getElementType().isFloatTy()) {
                                    F.getLoadGlobalVars().add(gv);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for(Function F : M.getFuncList()) {
            if(F.hasSideEffect()){
                spreadSideEffect(F);
            }
            if(F.useGlobalVars()){
                spreadUseGlobalVariables(F);
            }
        }
    }

    public void spreadSideEffect(Function F) {
        for(Function callerF : F.getCallerList()) {
            if(!callerF.hasSideEffect()){
                callerF.setSideEffect(true);
                spreadSideEffect(callerF);
            }
        }
    }

    public void spreadUseGlobalVariables(Function F) {
        for(Function callerF : F.getCallerList()) {
            callerF.getStoreGlobalVars().addAll(F.getStoreGlobalVars());
            if(!callerF.useGlobalVars()) {
                callerF.setUseGlobalVars(true);
                spreadUseGlobalVariables(callerF);
            }
        }
    }

    @Override
    public String getName() {
        return "InterproceduralAnalysis";
    }
}
