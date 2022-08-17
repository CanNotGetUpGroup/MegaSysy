package pass.passes;

import ir.Function;
import pass.ModulePass;

public class InterproceduralDCE extends ModulePass {

    public InterproceduralDCE() {
        super();
    }

    @Override
    public void runOnModule(Module M) {
        for (Function F : M.getFuncList()) {
            // 非Builtin函数
            if (F.isDefined()) {
                runOnFunction(F);
            }
        }
    }
}