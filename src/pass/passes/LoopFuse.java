package pass.passes;

import ir.Function;
import pass.FunctionPass;

public class LoopFuse extends FunctionPass {
    @Override
    public void runOnFunction(Function F) {

    }

    @Override
    public String getName() {
        return "Loop Fuse";
    }
}
