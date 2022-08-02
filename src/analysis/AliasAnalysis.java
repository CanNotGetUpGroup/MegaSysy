package analysis;

import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;
import org.antlr.v4.runtime.misc.Pair;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 确定两个指针是否指向内存同一对象
 * 一些关于内存地址的静态分析方法
 */
public class AliasAnalysis {

    private static Module M;
    private static HashMap<GlobalVariable, ArrayList<Function>> gv2func;
    private static HashMap<Function, ArrayList<GlobalVariable>> func2gv;

    public static Value getPointerValue(Value pointer) {
        while(pointer instanceof GetElementPtrInst || pointer instanceof LoadInst) {
            pointer = ((Instruction) pointer).getOperand(0);
        }
        if(pointer instanceof AllocaInst || pointer instanceof GlobalVariable) {
            if(pointer instanceof AllocaInst && ((AllocaInst) pointer).getAllocatedType().isPointerTy()) {
                for(Use use : pointer.getUseList()) {
                    if(use.getU() instanceof StoreInst) {
                        pointer = ((StoreInst) use.getU()).getOperand(1);
                    }
                }
            }
            return pointer;
        }
        return null;
    }
    
    public static boolean isGlobal(Value addr) {
        return addr instanceof GlobalVariable;
    }

    public static boolean isParam(Value addr) {
        if(addr instanceof AllocaInst) {
            return ((AllocaInst) addr).getAllocatedType().isPointerTy();
        }
        return false;
    }

    public static boolean isLocal(Value addr) {
        return !isGlobal(addr) && !isParam(addr);
    }

    private static HashSet<Function> visitedFunc;
    /**
     * Memory SSA
     * @param F
     */
    public static void runMemorySSA(Function F) {
        M = F.getParent();
        DominatorTree DT =  F.getDominatorTree();
        gv2func = new HashMap<>();
        func2gv = new HashMap<>();

        // Load User Functions
        for(GlobalVariable gv : M.getGlobalVariables()) {
           ArrayList<Function> userFuncs = new ArrayList<>();
           for(Use use : gv.getUseList()) {
                Function func = ((Instruction)use.getU()).getParent().getParent();
                if(!(func.getCallerList().isEmpty()) && (!func.getName().equals(("main"))) && (!userFuncs.contains(func))) {
                    userFuncs.add(func);
                }
           }
            gv2func.put(gv, userFuncs);
       }

        visitedFunc = new HashSet<>();
        for(Function func : M.getFuncList()) {
            ArrayList<GlobalVariable> gvs = new ArrayList<>();
            for(GlobalVariable gv : M.getGlobalVariables()) {
                visitedFunc.clear();
                if(isUsingGV(func, gv)) {
                    gvs.add(gv);
                }
            }
            func2gv.put(func, gvs);
        }
        // TODO: LOADSTORE Analysis
    }

    private static boolean isUsingGV(Function F, GlobalVariable gv) {
        if(visitedFunc.contains(F)){
            return false;
        }
        visitedFunc.add(F);
        if(gv2func.get(F).contains(gv)) {
            return true;
        }
        for(Function calleeFunc : F.getCalleeList()) {
            if(isUsingGV(calleeFunc, gv)) {
                return true;
            }
        }
        return false;
    }

    public static void clearMemorySSA(Function F) {
        for(BasicBlock BB : F.getBbList()) {
            for(Instruction I:BB.getInstList()) {
                switch(I.getOp()) {
                    
                }
            }
        }
    }
}