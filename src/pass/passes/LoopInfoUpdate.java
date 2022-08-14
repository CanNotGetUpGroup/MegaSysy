package pass.passes;

import ir.Function;
import pass.FunctionPass;

public class LoopInfoUpdate extends FunctionPass {

    @Override
    public String getName() {
        return "LoopInfoUpdate";
    }

    @Override
    public void runOnFunction(Function F) {
        F.getLoopInfo().computeLoopInfo(F);
    }
}
