package pass.passes;

import analysis.LoopInfo;
import ir.*;
import ir.instructions.Instructions;
import pass.FunctionPass;
import util.CloneMap;
import util.LoopUtils;
import util.MyIRBuilder;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * 转换成if do while结构
 */
public class AddCondPreBlock extends FunctionPass {
    private final MyIRBuilder builder=MyIRBuilder.getInstance();
    @Override
    public void runOnFunction(Function F) {
        HashSet<BasicBlock> Visited=new HashSet<>();
        LoopInfo LI=F.getLoopInfo();
        LI.computeLoopInfo(F);
//        LoopUtils.rearrangeBBOrder();
//        for(Loop loop:LI.getAllLoops()){
//            BasicBlock BB=loop.getLoopHeader();
//            BasicBlock trueBlock=BB.getSuccessor(0),falseBlock=BB.getSuccessor(1);
//            ArrayList<BasicBlock> preds=new ArrayList<>(BB.getPredecessors());
//            for(BasicBlock Pred:preds){
//                if(Pred==loop.getPreHeader()) continue;
//                CloneMap cm=new CloneMap();
//                cm.put(trueBlock,trueBlock);
//                cm.put(falseBlock,falseBlock);
//                BasicBlock newBB=new BasicBlock("",F,Pred);
//                Pred.getTerminator().remove();
//                builder.setInsertPoint(Pred);
//                builder.createBr(newBB);
//                createBr(newBB,BB,cm);
//            }
//        }
        for(BasicBlock BB:F.getBbList()){
            Visited.add(BB);
            if(BB.isCond){
                BasicBlock trueBlock=BB.getSuccessor(0),falseBlock=BB.getSuccessor(1);
                ArrayList<BasicBlock> preds=new ArrayList<>(BB.getPredecessors());
                for(BasicBlock Pred:preds){
                    if(Visited.contains(Pred)) continue;
                    CloneMap cm=new CloneMap();
                    cm.put(trueBlock,trueBlock);
                    cm.put(falseBlock,falseBlock);
                    BasicBlock newBB=new BasicBlock("",F,Pred);
                    Pred.getTerminator().remove();
                    builder.setInsertPoint(Pred);
                    builder.createBr(newBB);
                    createBr(newBB,BB,cm);
//                    BasicBlock preHeader=new BasicBlock("",F,BB);
//                    BB.getTerminator().COReplaceOperand(Pred,preHeader);
//                    builder.setInsertPoint(preHeader);
//                    builder.createBr(Pred);
                }
            }
        }
    }

    public void createBr(BasicBlock Src,BasicBlock Des,CloneMap cm){
        builder.setInsertPoint(Src);
        for (Instruction I : Des.getInstList()) {
            for(Value op:I.getOperandList()){
                if(cm.get(op)==null){
                    cm.put(op,op);
                }
            }
            Instruction copy = (Instruction) I.copy(cm);
            copy.getInstNode().insertIntoListEnd(Src.getInstList());
        }
    }

    @Override
    public String getName() {
        return "AddCondPreBlock";
    }
}
