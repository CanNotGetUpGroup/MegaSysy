package pass.passes;

import analysis.DominatorTree;
import ir.*;
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
 * 如果仅有一个前驱且该前驱仅有一个后继，将基本块与且前驱合并
 * 消除只有一个前驱的基本块的PHI节点
 * 消除仅包含无条件分支的基本块
 */
public class SimplifyCFG extends FunctionPass {
    DominatorTree DT;
    @Override
    public void runOnFunction(Function F) {
        DominatorTree DT = new DominatorTree(F);
        boolean changed=mergeEmptyReturnBlocks(F);
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
                RetBlockPHI= PHIInst.create(Ret.getOperand(0).getType(), RetBlock.getPredecessorsNum(),"%merge", RetBlock.front());
                for (BasicBlock pred : RetBlock.getPredecessors()) {
                    RetBlockPHI.addIncoming(InVal,pred);
                }
                RetBlock.getTerminator().setOperand(0, RetBlockPHI);
            }
            RetBlockPHI=(PHIInst)RetBlock.front();

            RetBlockPHI.addIncoming(Ret.getOperand(0), BB);
            BB.getTerminator().remove();
            BranchInst.create(RetBlock, BB);
        }

        return Changed;
    }

    public boolean iterativelySimplify(Function F) {
        boolean Changed = false;
        boolean LocalChange = true;

        ArrayList<Pair<BasicBlock,BasicBlock >> Edges=new ArrayList<>();
//        FindFunctionBackedges(F, Edges);
        Set<BasicBlock> LoopHeaders = new HashSet<>();
        for (int i = 0, e = Edges.size(); i != e; ++i)
            LoopHeaders.add(Edges.get(i).b);

        while (LocalChange) {
            LocalChange = false;

            // Loop over all of the basic blocks and remove them if they are unneeded.
            for (BasicBlock BB:F.getBbList()) {
                if (simplifyCFG(BB,LoopHeaders)) {
                    LocalChange = true;
                }
            }
            Changed |= LocalChange;
        }
        return Changed;
    }

    public boolean simplifyCFG(BasicBlock BB,Set<BasicBlock> Loop){
        boolean ret=false;
        //化简终结指令
        ret|=Folder.constantFoldTerminator(BB);
        DT.update(BB.getParent());
        //如果仅有一个前驱且该前驱仅有一个后继，将基本块与且前驱合并
        if(mergeBlockIntoPredecessor(BB)){
            return true;
        }

        return ret;
    }

    public boolean mergeBlockIntoPredecessor(BasicBlock BB){
        if(BB.getOnlyPredecessor()==null) return false;
        BasicBlock Pred=BB.getPredecessor(0);
        if(Pred==BB) return false;
        if(Pred.getOnlySuccessor()==null) return false;

        for(Instruction I:BB.getInstList()){
            if(!(I instanceof PHIInst)){
                break;
            }
            if(I.getOperandList().contains(I)){
                return false;
            }
        }
        foldSinglePHIInst(BB);

        Pred.getTerminator().remove();
        BB.replaceAllUsesWith(Pred);
        Pred.getInstList().mergeList(BB.getInstList());
        BB.remove();
        return true;
    }

    public boolean foldSinglePHIInst(BasicBlock BB){
        if(!(BB.front() instanceof PHIInst)) return false;
        for(Instruction I:BB.getInstList()){
            if(!(I instanceof PHIInst)){
                break;
            }
            PHIInst PI=(PHIInst)I;
            if(PI.getOperand(0)!=PI){
                PI.replaceAllUsesWith(PI.getOperand(0));
            }else{
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
