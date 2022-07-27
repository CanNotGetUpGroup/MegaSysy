package pass.passes;

import ir.*;
import ir.Module;
import pass.ModulePass;
import ir.instructions.Instructions.*;
import analysis.DominatorTree.TreeNode;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.*;

/**
 * 求与Br Ret Store和有附加影响的Call的指令相关的闭包，删除闭包外指令
 * 删除不可达函数
 */
public class DeadCodeEmit extends ModulePass {

    Set<Instruction> usefulInstSet = new HashSet<>();

    public DeadCodeEmit() {
        super();
    }

    @Override
    public void runOnModule(Module M){
        ArrayList<Function> deadFunc = new ArrayList<>();
        for(Function F:M.getFuncList()){
            // 非Builtin函数
            if(F.isDefined()) {
                functionDCE(F);
            }

            if(F.getCallerList().isEmpty() && !F.getName().equals("main")) {
                deadFunc.add(F);
            }
        }
        for(Function F : deadFunc) {
            F.remove();
        }

        // TODO: DEBUG for .ll test
        M.rename();
    }

    public void functionDCE(Function F) {
        // removeDeadStore

        usefulInstSet.clear();
        for(BasicBlock BB : F.getBbList()){
            for(Instruction I:BB.getInstList()) {
                if(isUsefulInst(I)) {
                    findClosure(I);
                }
            }
        }

        for(BasicBlock BB : F.getBbList()){
            for(Instruction I:BB.getInstList()) {
                if(!usefulInstSet.contains(I)){
                    I.remove();
                }
            }
        }
    }

    /**
     * 只有与Br Ret Store和有附加影响的Call的指令相关的闭包是有效指令
     * @param I
     * @return
     */
    public boolean isUsefulInst(Instruction I) {
        return switch (I.getOp()) {
            case Br,Ret,Store -> true;
            case Call -> I.getFunction().hasSideEffect();
            default -> false;
        };
    }

    /**
     * 递归扩展指令闭包,并添加到usefulInstSet
     * @param I
     */
    public void findClosure(Instruction I) {
        if(usefulInstSet.contains(I)) {
            return;
        }
        usefulInstSet.add(I);
        for(Value op : I.getOperandList()) {
            if(op instanceof Instruction) {
                findClosure((Instruction) op);
            }
        }
    }

    @Override
    public String getName() {
        return "DeadCodeEmit";
    }
}