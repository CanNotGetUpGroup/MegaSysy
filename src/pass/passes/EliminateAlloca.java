package pass.passes;

import analysis.PointerInfo;
import ir.*;
import ir.instructions.Instructions;
import pass.FunctionPass;

public class EliminateAlloca extends FunctionPass {
    @Override
    public void runOnFunction(Function F) {
        boolean ret;
        do{
            ret=false;
            for(BasicBlock BB:F.getBbList()){
                for(Instruction I:BB.getInstList()){
                    ret|=eliminateUndef(I);
                    if(I instanceof Instructions.AllocaInst)
                        ret|=allocaGVN((Instructions.AllocaInst) I);
                }
            }
        }while (ret);
        new DeadCodeEmit().functionDCE(F);
    }

    /**
     * 删去只被store过的alloca
     */
    public boolean allocaGVN(Instructions.AllocaInst AI) {
        PointerInfo PI = new PointerInfo(AI);
        PI.calculateInfo(AI);
        if (!PI.isLoaded()) {
            AI.remove();
            return true;
        }
        return false;
    }

    public boolean eliminateUndef(Instruction I){
        if(I instanceof Instructions.PHIInst) return false;
        for(Value v:I.getOperandList()){
            if(Constants.UndefValue.isUndefValue(v)){
                I.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Eliminate Alloca and Undef";
    }
}
