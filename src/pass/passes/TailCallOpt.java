package pass.passes;

import ir.Function;
import pass.FunctionPass;

public class TailCallOpt extends FunctionPass {

    @Override
    public String getName() {
        return "TailCallOpt";
    }

    @Override
    public void runOnFunction(Function F) {
        if(F.getName().equals("main")) {
            return;
        }
        if(F.getReturnBlock().getPredecessors().size() > 1) {
            return;
        }
        var inst = F.getReturnBlock().getPredecessors().get(0).getTerminator().getInstNode().getPrev().getVal();
        System.out.println(inst);
    }
}
