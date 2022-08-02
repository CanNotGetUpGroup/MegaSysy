package analysis;

import ir.*;
import ir.Module;
import ir.Constants.ConstantArray;
import ir.DerivedTypes.ArrayType;
import ir.DerivedTypes.PointerType;
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

    public static Value getArrayValue(Value pointer) {
        while(pointer instanceof GetElementPtrInst || pointer instanceof LoadInst) {
            pointer = ((Instruction) pointer).getOperand(0); // Addr/Ptr
        }
        if(pointer instanceof AllocaInst || pointer instanceof GlobalVariable) {
            if(pointer instanceof AllocaInst && ((AllocaInst) pointer).getAllocatedType().isPointerTy()) {
                for(Use use : pointer.getUseList()) {
                    if(use.getU() instanceof StoreInst) {
                        pointer = ((Instruction) use.getU()).getOperand(1);
                    }
                }
            }
            return pointer;
        }else {
            return null;
        }
    }

    /**
     * 判断GlobalArray和ParamArray
     * 通过比较dims判断的，感觉比较取巧 Ayame的写法
     * @param global
     * @param param
     * @return
     */
    public static boolean aliasGlobalParam(Value global, Value param) {
        if(!isGlobal(global) || !isParam(param)){
            return false;
        }

        int dimsn1,dimsn2;
        ArrayList<Integer> dims1,dims2;


        ConstantArray globalArray = (ConstantArray) ((GlobalVariable) global).getOperand(0); // get InitVal
        dims1 = globalArray.getDims();
        dimsn1 = dims1.size();
        for(var i= dimsn1-2; i >= 0; i--) {
            dims1.set(i, dims1.get(i) * dims1.get(i+1));
        }

        AllocaInst alloca = (AllocaInst) param;
        PointerType ptrTy = (PointerType) alloca.getAllocatedType();
        if(ptrTy.getElementType().isInt32Ty() || ptrTy.getElementType().isFloatTy()) {
            return true;
        }

        ArrayType arrayType = (ArrayType) ptrTy.getElementType();
        dims2 = arrayType.getDims();
        dimsn2 = dims2.size();
        for(var i= dimsn2-2; i >= 0; i--) {
            dims2.set(i, dims2.get(i) * dims2.get(i+1));
        }

        int minDims = Math.min(dimsn1, dimsn2);
        for(var i=0; i<minDims; i++) {
            if(i==0 && minDims==dimsn2) {
                continue;
            }
            if(dims1.get(i+dimsn1-minDims) != dims2.get(i+dimsn2-minDims)){
                return false;
            }
        }
        return true;
    }

    public static boolean alias(Value p1, Value p2) {
        // same type
        if((isGlobal(p1)&&isGlobal(p2)) || (isParam(p1)&&isParam(p2)) || (isLocal(p1)&&isLocal(p2))) {
            return p1 == p2;
        }
        // global-param
        if(isGlobal(p1) && isParam(p2) && ((GlobalVariable) p1).getOperand(0) instanceof ConstantArray) {
            return aliasGlobalParam(p1, p2);
        }
        if(isGlobal(p2) && isParam(p1) && ((GlobalVariable) p2).getOperand(0) instanceof ConstantArray) {
            return aliasGlobalParam(p2, p1);
        }
        return false;
    }
    
    public static boolean callAlias(Value pointer, CallInst callInst) {
        if(isParam(pointer)) {
            return true;
        }
        if(isGlobal(pointer) && (func2gv.get(callInst.getCalledFunction()).contains(pointer))) {
            return true;
        }

        for (Value arg : callInst.getArgs()) {
            if(arg instanceof GetElementPtrInst) {
                if(alias(pointer, getArrayValue(arg))) {
                    return true;
                }
            }
        }
        return false;
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
            func2gv.put(func, new ArrayList<>());
            for(GlobalVariable gv : M.getGlobalVariables()) {
                visitedFunc.clear();
                if(isUsingGV(func, gv)) {
                    func2gv.get(func).add(gv);
                }
            }
        }
        // TODO: LOADSTORE Analysis
    }

    private static boolean isUsingGV(Function F, GlobalVariable gv) {
        if(visitedFunc.contains(F)){
            return false;
        }
        visitedFunc.add(F);
        if(func2gv.get(F).contains(gv)) {
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