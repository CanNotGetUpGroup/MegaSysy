package pass.passes;

import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Module;
import ir.instructions.Instructions.*;
import pass.FunctionPass;

public class LocalArrayPromote extends FunctionPass {
    public LocalArrayPromote() {
        super();
    }

    @Override
    public void runOnFunction(Function F) {
        for(BasicBlock BB:F.getBbList()){
            for(Instruction I:BB.getInstList()){
                if(I instanceof AllocaInst){

                }
            }
        }
    }

    @Override
    public void runOnModule(Module M) {
        super.runOnModule(M);
    }

    @Override
    public String getName() {
        return "Local Array Promote";
    }
}
