package pass.passes;

import analysis.DominatorTree;
import ir.Function;
import ir.Loop;
import pass.FunctionPass;

import java.util.ArrayList;

public class LoopUnroll extends FunctionPass {
    private DominatorTree DT;
    private ArrayList<Loop> loops;
    @Override
    public void runOnFunction(Function F) {
        F.getLoopInfo().computeLoopInfo(F);
        loops=F.getLoopInfo().getAllLoops();
        if(loops.isEmpty()) return;
        DT=F.getAndUpdateDominatorTree();
        for(Loop l:loops){

        }
    }

    public void runOnLoop(Loop L){

    }

    @Override
    public String getName() {
        return "Loop Unroll";
    }
}
