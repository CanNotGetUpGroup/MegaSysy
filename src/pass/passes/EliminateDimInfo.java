package pass.passes;

import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Value;
import ir.instructions.Instructions;
import pass.FunctionPass;

/**
 * 清除掉GEP的DimInfo（在ArraySSA使用完后）
 */
public class EliminateDimInfo extends FunctionPass {
    @Override
    public void runOnFunction(Function F) {
        for(BasicBlock BB:F.getBbList()){
            for(Instruction I:BB.getInstList()){
                if(I instanceof Instructions.GetElementPtrInst){
                    for(Value v:((Instructions.GetElementPtrInst) I).getDimInfoDirectly().getOperandList()){
                        v.dropUsesAsValue();
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Eliminate Dim Info";
    }
}
