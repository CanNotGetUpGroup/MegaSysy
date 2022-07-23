package pass.passes;

import analysis.DominatorTree;
import ir.*;
import ir.Module;
import ir.instructions.Instructions.*;
import org.antlr.v4.runtime.misc.Pair;
import pass.FunctionPass;
import util.Folder;
import util.IList;
import util.IListIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 消除不可达的基本块
 * 化简终结指令（br i1 %1, label %2, label %2 或 br i1 1, label %2, label %3）-> (br label %2)
 * 基本块与前驱合并
 * 消除只有一个前驱的基本块的PHI节点
 * 合并无条件跳转指令且能合并的基本块
 */
public class SimplifyCFG extends FunctionPass {
    DominatorTree DT;
    Function F;

    @Override
    public void runOnFunction(Function F) {
        this.F=F;
        DT = new DominatorTree(F);
        boolean changed = mergeEmptyReturnBlocks(F);
        changed|=iterativelySimplify(F);
        if(!changed) return;
        do {
            changed = iterativelySimplify(F);
        } while (changed);
        Module.getInstance().rename(F);
    }

    public boolean mergeEmptyReturnBlocks(Function F) {
        boolean Changed = false;

        BasicBlock RetBlock = null;

        // 查找只有return指令或phi和return指令的基本块
        for (BasicBlock BB : F.getBbList()) {
            if (!(BB.getTerminator() instanceof ReturnInst)) continue;
            ReturnInst Ret = (ReturnInst) (BB.getTerminator());

            // 检查基本块中除了Ret外是否只有一个决定返回值的phi指令
            if (Ret != BB.front()) {
                Instruction I = BB.front();
                if (!(I instanceof PHIInst) || Ret.getNumOperands() == 0 || Ret.getOperand(0) != I) {
                    continue;
                }
            }
            // 记录第一个返回的基本块
            if (RetBlock == null) {
                RetBlock = BB;
                continue;
            }
            Changed = true;

            // BB与RetBlock相同
            if (Ret.getNumOperands() == 0 || Ret.getOperand(0) == (RetBlock.getTerminator()).getOperand(0)) {
                BB.replaceAllUsesWith(RetBlock);
                BB.remove();
                continue;
            }

            PHIInst RetBlockPHI;
            if (!(RetBlock.front() instanceof PHIInst)) {
                Value InVal = (RetBlock.getTerminator()).getOperand(0);
                RetBlockPHI = PHIInst.create(Ret.getOperand(0).getType(), RetBlock.getPredecessorsNum(), "%merge", RetBlock.front());
                for (BasicBlock pred : RetBlock.getPredecessors()) {
                    RetBlockPHI.addIncoming(InVal, pred);
                }
                RetBlock.getTerminator().setOperand(0, RetBlockPHI);
            }
            RetBlockPHI = (PHIInst) RetBlock.front();

            RetBlockPHI.addIncoming(Ret.getOperand(0), BB);
            BB.getTerminator().remove();
            BranchInst.create(RetBlock, BB);
        }

        return Changed;
    }

    public boolean iterativelySimplify(Function F) {
        boolean Changed = false;
        boolean LocalChange = true;

        while (LocalChange) {
            LocalChange = false;
            for (BasicBlock BB : F.getBbList()) {
                if (simplifyCFG(BB)) {
                    LocalChange = true;
                }
            }
            DT.update(F);
            Changed |= LocalChange;
        }
        return Changed;
    }

    public boolean simplifyCFG(BasicBlock BB) {
        boolean ret = false;
        //化简终结指令
        ret = Folder.constantFoldTerminator(BB);
        //如果仅有一个前驱且该前驱仅有一个后继，将基本块与且前驱合并
        if (mergeBlockIntoPredecessor(BB)) {
            return true;
        }
        if (BB.getTerminator() instanceof BranchInst) {
            BranchInst BI = (BranchInst) BB.getTerminator();
            if (BI.getNumOperands() == 1) {
                //消除仅包含无条件分支的基本块
                if (BB.getFirstNonPHI() == BB.getTerminator()) {
                    ret|=removeEmptyUnCondBrBlock(BB);
                }
            }
        }

        return ret;
    }

    /**
     * 合并无条件跳转指令且能合并的基本块
     */
    public boolean removeEmptyUnCondBrBlock(BasicBlock BB) {
        if (BB == BB.getParent().getEntryBB()&&BB.getSuccessor(0).getPredecessorsNum()!=1) return false;
        BasicBlock Succ = BB.getSuccessor(0);
        if (BB == Succ) return false;
        //判断是否能合并
        BasicBlock OnlyBB = Succ.getOnlyPredecessor();
        if (OnlyBB == null) {
            Set<BasicBlock> PredBB = new HashSet<>(BB.getPredecessors());
            for (Instruction I : Succ.getInstList()) {
                if (!(I instanceof PHIInst)) {
                    break;
                }
                PHIInst PI = (PHIInst) I;
                Value val = PI.getIncomingValueByBlock(BB);
                //来自BB的phi，需要检查BB与Succ是否存在共同前驱。若存在，
                //检查前驱->Succ的phi的值与前驱->BB->Succ是否相同，相同（或存在undef）则可以合并
                if (val instanceof PHIInst && ((PHIInst) val).getParent() == BB) {
                    PHIInst BBPI = (PHIInst) val;
                    for (int i = 0; i < PI.getNumOperands(); i++) {
                        BasicBlock IBB = PI.getIncomingBlock(i);
                        if (PredBB.contains(IBB)) {
                            Value A = BBPI.getIncomingValueByBlock(IBB);
                            Value B = PI.getIncomingValue(i);
                            if (!(A == B || Constants.UndefValue.isUndefValue(A) || Constants.UndefValue.isUndefValue(B))) {
                                return false;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < PI.getNumOperands(); i++) {
                        BasicBlock IBB = PI.getIncomingBlock(i);
                        if (PredBB.contains(IBB)) {
                            Value B = PI.getIncomingValue(i);
                            if (!(val == B || Constants.UndefValue.isUndefValue(val) || Constants.UndefValue.isUndefValue(B))) {
                                return false;
                            }
                        }
                    }
                }
            }
            for (Instruction I : BB.getInstList()) {
                if (!(I instanceof PHIInst)) {
                    break;
                }
                PHIInst PI = (PHIInst) I;
                for (Use U : PI.getUseList()) {
                    if (U.getU() instanceof PHIInst && ((PHIInst) U.getU()).getIncomingBlock(U) != BB) {
                        return false;
                    } else if (!(U.getU() instanceof PHIInst)) {
                        return false;
                    }
                }
            }
        }

        if (Succ.front() instanceof PHIInst) {
            ArrayList<BasicBlock> BBPreds = BB.getPredecessors();

            for (Instruction I : Succ.getInstList()) {
                if (!(I instanceof PHIInst)) {
                    break;
                }
                PHIInst PI = (PHIInst) I;
                PI.redirectValuesFromPredecessors(BB, BBPreds);
            }
        }

        boolean removeTerminator=false;
        if (Succ.getOnlyPredecessor() != null) {
            BB.getTerminator().remove();
            removeTerminator=true;
            Succ.getInstList().splice(Succ.getFirstNonPHI().getInstNode().getIterator(), BB.getInstList());
        } else {
            for (Instruction I : BB.getInstList()) {
                if (I instanceof PHIInst) {
                    I.remove();
                } else {
                    break;
                }
            }
        }

        // 将跳转到BB替换为Succ
        BB.replaceAllUsesWith(Succ);
        BB.remove(removeTerminator);
        return true;
    }


    /**
     * 基本块与前驱合并
     */
    public boolean mergeBlockIntoPredecessor(BasicBlock BB) {
        if (BB.getOnlyPredecessor() == null) return false;
        BasicBlock Pred = BB.getPredecessor(0);
        if (Pred == BB) return false;
        if (Pred.getOnlySuccessor() == null) return false;

        for (Instruction I : BB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            if (I.getOperandList().contains(I)) {
                return false;
            }
        }
        foldSinglePHIInst(BB);

        Pred.getTerminator().remove();
        BB.replaceAllUsesWith(Pred);
        Pred.getInstList().mergeList(BB.getInstList());
        BB.remove(true);
        return true;
    }

    /**
     * 消除只有一个前驱的基本块的PHI节点
     */
    public boolean foldSinglePHIInst(BasicBlock BB) {
        if (!(BB.front() instanceof PHIInst)) return false;
        for (Instruction I : BB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            PHIInst PI = (PHIInst) I;
            if (PI.getOperand(0) != PI) {
                PI.replaceAllUsesWith(PI.getOperand(0));
            } else {
                PI.replaceAllUsesWith(Constants.UndefValue.get(PI.getType()));
            }
            PI.remove();
        }
        return true;
    }

    @Override
    public String getName() {
        return "Simplify CFG";
    }
}
