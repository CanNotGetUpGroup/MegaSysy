package pass.passes;

import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import pass.FunctionPass;

public class Travel2Debug extends FunctionPass {

        @Override
        public String getName() {
            return "Debug";
        }

        @Override
        public void runOnFunction(Function F) {
            for (BasicBlock BB : F.getBbList()) {
                for (Instruction I : BB.getInstList()) {
                    if(I.isTerminator()){
                        System.out.println(I.toString());
                    }
                }
            }
        }
}
