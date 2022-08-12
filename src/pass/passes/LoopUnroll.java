package pass.passes;

import ir.Function;
import ir.Loop;
import pass.FunctionPass;

public class LoopUnroll extends FunctionPass {
    @Override
    public void runOnFunction(Function F) {

    }

    public void runOnLoop(Loop L){

    }

    @Override
    public String getName() {
        return "Loop Unroll";
    }
}
