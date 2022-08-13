package pass.passes;

import analysis.DominatorTree;
import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Loop;
import ir.instructions.Instructions.*;
import pass.FunctionPass;
import util.LoopUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class LoopUnroll extends FunctionPass {
    private DominatorTree DT;

    @Override
    public void runOnFunction(Function F) {
        F.getLoopInfo().computeLoopInfo(F);
        ArrayList<Loop> loops = F.getLoopInfo().getTopLevelLoops();
        if(loops.isEmpty()) return;
        DT=F.getAndUpdateDominatorTree();
        new LCSSA().runOnFunction(F);
        Queue<Loop> WorkList=new LinkedList<>();

        for(Loop l: loops){
            LoopUtils.addLoopToWorkList(l,WorkList);
        }
        while(!WorkList.isEmpty()){
            Loop L=WorkList.poll();
            tryToUnrollLoop(L);
        }
    }

    public void tryToUnrollLoop(Loop L){
        if(!L.isSafeToCopy()){
            return;
        }

        HashSet<BasicBlock> Visited=new HashSet<>();
        LoopUtils.rearrangeBBOrder(L,Visited);

    }

    public void UnrollLoop(Loop L){
        if(!L.isSimpleForLoop()){
            return;
        }
        BasicBlock Header=L.getLoopHeader();
        BasicBlock LatchBB=L.getSingleLatchBlock();
        ArrayList<BasicBlock> ExitBB=L.getExitBlocks();
        ArrayList<BasicBlock> OriBBs=L.getBbList();

    }

    @Override
    public String getName() {
        return "Loop Unroll";
    }
}
