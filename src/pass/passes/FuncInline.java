package pass.passes;

import pass.ModulePass;
import pass.Pass;

public class FuncInline extends ModulePass {
    public FuncInline() {
        super();
    }

    @Override
    public void runOnModule(Module M) {

    }

    @Override
    public String getName() {
        return "Function Inline";
    }
}
