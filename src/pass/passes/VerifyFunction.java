package pass.passes;

import analysis.LoopInfo;
import ir.*;
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
            for(Instruction I:BB.getInstList()){
                if(I instanceof Instructions.GetElementPtrInst){
                    if(I.getType() instanceof DerivedTypes.PointerType
                            &&!((DerivedTypes.PointerType) I.getType()).getElementType().isIntegerTy()){
                        if(((Instructions.GetElementPtrInst) I).getDimInfoDirectly().getNumOperands()!=0){
                            System.out.println(I+" should not have dimInfo "
                                    +((Instructions.GetElementPtrInst) I).getDimInfoDirectly());
                        }
                    }
                }
            }
            LoopInfo LI=F.getLoopInfo();
            LI.computeLoopInfo(F);
            for(Loop loop:LI.getAllLoops()){
                BasicBlock preHeader=loop.getPreHeader();
                if(loop.isSimpleForLoop()){
                    if(preHeader==null){
                        System.out.println("(loop header:"+loop.getLoopHeader()
                                +") is simple loop, but don't have PreHeader!");
                    }
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
