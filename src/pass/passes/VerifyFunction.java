package pass.passes;

import ir.BasicBlock;
import ir.Function;
import ir.instructions.Instructions;
import pass.FunctionPass;

public class VerifyFunction extends FunctionPass {
    @Override
    public void runOnFunction(Function F) {
        for(BasicBlock BB:F.getBbList()){
            for(Instructions.PHIInst phi:BB.getPHIs()){
                if(!phi.getBlocks().contains(BB)){
                    System.out.println(BB+" should dominate "+phi);
                }
            }
        }
    }

    /**
     * 调试时用
     */
    public static String verifyFunction(Function F){
        StringBuilder sb=new StringBuilder();
        for(BasicBlock BB:F.getBbList()){
            for(Instructions.PHIInst phi:BB.getPHIs()){
                if(!phi.getBlocks().contains(BB)){
                    sb.append(BB).append(" should dominate ").append(phi).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
