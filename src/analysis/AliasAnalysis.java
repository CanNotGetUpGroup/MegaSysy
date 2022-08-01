package analysis;

import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;
import org.antlr.v4.runtime.misc.Pair;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 确定两个指针是否指向内存同一对象
 * 一些关于内存地址的静态分析方法
 */
public class AliasAnalysis {

    private static Module M;

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

    /**
     * Memory SSA
     * @param F
     */
    public static void run(Function F) {
        M = F.getParent();
        F.getDominatorTree();

        // loadUserFuncs();
    }

    public static void clear(Function F) {
        for(BasicBlock BB : F.getBbList()) {
            for(Instruction I:BB.getInstList()) {
                switch(I.getOp()) {
                    
                }
            }
        }
    }
}