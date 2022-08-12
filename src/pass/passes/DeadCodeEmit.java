package pass.passes;

import analysis.MemoryAccess;
import ir.*;
import ir.Module;
import ir.Instruction.Ops;
import pass.ModulePass;
import ir.instructions.Instructions.*;
import analysis.AliasAnalysis;
import analysis.DominatorTree.TreeNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import java.util.Set;

/**
 * 求与Br Ret Store和有附加影响的Call的指令相关的闭包，删除闭包外指令
 * 删除不可达函数
 */
public class DeadCodeEmit extends ModulePass {

    Set<Instruction> usefulInstSet = new HashSet<>();
    Stack<Instruction> stk = new Stack<>();

    public DeadCodeEmit() {
        super();
    }

    @Override
    public void runOnModule(Module M){
        ArrayList<Function> deadFunc = new ArrayList<>();
        for(Function F:M.getFuncList()){
            // 非Builtin函数
            if(F.isDefined()) {
                // TODO: 暂时先在这里初始化func-gv映射 StoreDel要用
                AliasAnalysis.runMemorySSA(F);
                functionDCE(F);
            }

            if(F.getCallerList().isEmpty() && !F.getName().equals("main")) {
                deadFunc.add(F);
            }
        }
        for(Function F : deadFunc) {
            F.remove();
        }

    }

    public void functionDCE(Function F) {
        for(BasicBlock BB : F.getBbList()) {
            for(Instruction I : BB.getInstList()) {
                MemoryAccess MA=AliasAnalysis.MSSA.getMemoryAccess(I);
                if(MA instanceof MemoryAccess.MemoryDef){
                    MemoryAccess.MemoryDef MD=(MemoryAccess.MemoryDef)MA;
                    boolean delete=false;
                    if((AliasAnalysis.isLocal(MD.getPointer()))){//不删除对global和param的定义
                        for (Use u : MD.getUseList()) {
                            User user = u.getU();
                            if (user instanceof MemoryAccess.MemoryUse) {
                                break;
                            } else if (user instanceof MemoryAccess.MemoryDef) {
                                if (((MemoryAccess.MemoryDef) user).getMemoryInstruction() instanceof CallInst) {
                                    CallInst CI = (CallInst) ((MemoryAccess.MemoryDef) user).getMemoryInstruction();
                                    if (AliasAnalysis.MSSA.callAlias(CI, AliasAnalysis.getArrayValue(MD.getPointer()))) {
                                        break;
                                    }
                                } else {//被store覆盖
                                    if ((MD.getMemoryInstruction() instanceof StoreInst) &&
                                            ((MemoryAccess.MemoryDef) user).getMemoryInstruction().getOperand(1).equals(
                                                    MD.getMemoryInstruction().getOperand(1)
                                            )) {//对数组的不同位置进行的store，不可覆盖
                                        delete=true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if(delete){
                        I.remove();
                        MA.replaceAllUsesWith(((MemoryAccess.MemoryDef) MA).getDefiningAccess());
                        MA.remove();
                    }
                }
            }
        }

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
            case Call -> ((Function)I.getOperand(0)).hasSideEffect();
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
        stk.add(I);
        findClosureDFS();
    }

    public void findClosureDFS() {
        while(!stk.isEmpty()) {
            Instruction I = stk.pop();
            for(Value op : I.getOperandList()) {
                if(op instanceof Instruction && !usefulInstSet.contains((Instruction) op)) {
                    usefulInstSet.add((Instruction) op);
                    stk.add((Instruction) op);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "DeadCodeEmit";
    }
}