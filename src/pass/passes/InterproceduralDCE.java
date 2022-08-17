package pass.passes;

import ir.Module;
import pass.ModulePass;

public class InterproceduralDCE extends ModulePass {

    public InterproceduralDCE() {
        super();
    }

    @Override
    public String getName() {
        return "InterproceduralDCE";
    }

    @Override
    public void runOnModule(Module M) {
        for (var func : M.getFuncList()) {
            if (!func.isDefined()) {
                continue;
            }

        }
    }

}
