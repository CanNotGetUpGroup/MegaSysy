package pass.passes;

import ir.*;
import ir.Module;
import ir.Instruction.Ops;
import org.antlr.v4.runtime.misc.Pair;
import pass.ModulePass;
import ir.instructions.Instructions.*;

import java.util.*;

import analysis.DominatorTree;
import util.Folder;
import util.IList;
import util.IListIterator;
import util.MyIRBuilder;

public class GVNGCM extends ModulePass {

    private ArrayList<Pair<Value, Value>> valueTable = new ArrayList<>();
    private Set<Instruction> visInsts = new HashSet<>();

    public GVNGCM() {
        super();
    }

    @Override
    public void runOnModule(Module M){
        for(Function F:M.getFuncList()){
            // 非Builtin函数
            if(F.isDefined()) {
                functionGVNGCM(F);
            }
        }
    }

    public void functionGVNGCM(Function F) {
        boolean shouldContinue=true;
        while (shouldContinue){
            shouldContinue=functionGVN(F);
            new DeadCodeEmit().runOnModule(Module.getInstance());
            shouldContinue|=new SimplifyCFG().run(F);
        }
//        functionGCM(F);
        Module.getInstance().rename(F);
    }

    public boolean functionGVN(Function F) {
        boolean ret=false;
        valueTable.clear();

        //直接利用DT中的逆后序遍历信息
        for(var node : F.getAndUpdateDominatorTree().getReversePostOrder()) {
            ret|=basicBlockGVN(node.BB);
        }
        return ret;
    }

    public boolean basicBlockGVN(BasicBlock BB) {
        boolean ret=combinePhi(BB);
        ArrayList<Instruction> deadInst=new ArrayList<>();
        IListIterator<Instruction,BasicBlock> It= (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator(),It_pre= (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
        Instruction I=It.next();
        while (It.hasNext()) {
            ret|=instructionGVN(I,deadInst);
            if(deadInst.isEmpty()){
                I=It.next();
                continue;
            }
            if(I!=BB.getInstList().getFirst().getVal()) {
                It.previous();
                It.previous();
            }
//            System.out.println("remove "+deadInst.size()+" instructions");
            for(Instruction J:deadInst){
                J.remove();
            }
            deadInst.clear();
            if(I==BB.getInstList().getFirst().getVal()) It= (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
            else I=It.next();
        }
        return ret;
    }

    public boolean combinePhi(BasicBlock BB){
        boolean ret=false;
        IListIterator<Instruction,BasicBlock> It= (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
        Instruction I=It.next();
        while (I instanceof PHIInst) {
            PHIInst PI = (PHIInst) I;
            I = It.next();
            IListIterator<Instruction, BasicBlock> Jt = BB.getInstList().iterator(I);
            Instruction J = Jt.next();
            while (true) {
                if (!(J instanceof PHIInst)) {
                    break;
                }
                if (PI.isSameWith(J)) {
                    ret=true;
                    J.replaceAllUsesWith(PI);
                    J.remove();
                    It = (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
                    I = It.next();
                    break;
                }
                J = Jt.next();
            }
        }
        return ret;
    }

    public boolean instructionGVN(Instruction I,ArrayList<Instruction> deadInst) {
        boolean ret=false;
        Value V= Folder.simplifyInstruction(I);
        if(V != null){
            I.replaceAllUsesWith(V);
            deadInst.add(I);
            ret=true;
        }

        return ret;
    }

    public void functionGCM(Function F) {

        // TODO: CALL without GEP
        F.getLoopInfo().computeLoopInfo(F);
        ArrayList<Instruction> insts = new ArrayList<>();
        for(BasicBlock BB : F.getBbList()) {
            for(Instruction I : BB.getInstList()) {
                insts.add(I);
            }
        }
        visInsts.clear();
        for(Instruction I : insts) {
            scheduleEarly(I, F);
        }
        visInsts.clear();
        for(Instruction I : insts) {
            scheduleLate(I, F);
        }
    }

    public void scheduleEarly(Instruction I, Function F) {
        DominatorTree DT = F.getDominatorTree();
        if(!visInsts.contains(I)) {
            visInsts.add(I);

            if(scheduleAble(I)) {
                I.getInstNode().remove();
                F.getEntryBB().getInstList().insertBeforeEnd(I.getInstNode());
            }
            if(Instruction.isBinary(I.getOp()) || I.getOp().equals(Ops.Load) || I.getOp().equals(Ops.GetElementPtr)) {
                for(Value op : I.getOperandList()) {
                    if(op instanceof Instruction) {
                        Instruction opInst = (Instruction)op;
                        scheduleEarly(opInst, F);
                        if(DT.getNode(opInst.getParent()).level > DT.getNode(I.getParent()).level) {
                            I.getInstNode().remove();
                            opInst.getParent().getInstList().insertBeforeEnd(I.getInstNode());
                        }
                    }
                }
            }

            if(I.getOp().equals(Ops.Call) && ((CallInst) I).withoutGEP()) {
                // skip op0
                for(var i=1; i<I.getNumOperands(); i++) {
                    Value op = I.getOperand(i);
                    if(op instanceof Instruction) {
                        Instruction opInst = (Instruction) op;
                        scheduleEarly(opInst, F);
                        if(DT.getNode(opInst.getParent()).level > DT.getNode(I.getParent()).level) {
                            I.getInstNode().remove();
                            opInst.getParent().getInstList().insertBeforeEnd(I.getInstNode());
                        }
                    }
                }
            }
        }
    }

    public void scheduleLate(Instruction I, Function F) {
        DominatorTree DT = F.getDominatorTree();

        if(scheduleAble(I) && !visInsts.contains(I)) {
            visInsts.add(I);

            BasicBlock curBB = null;
            for(Use use : I.getUseList()) {
                User user = use.getU();
                if(user instanceof Instruction) {
                    Instruction userInst = (Instruction) user;
                    scheduleLate(userInst, F);
                    BasicBlock userBB = userInst.getParent();
                    if(userInst.getOp().equals(Ops.PHI)) {
                        int index = 0;
                        for(Value val : userInst.getOperandList()) { // PHI incoming vals
                            if(val instanceof Instruction && val.getUseList().contains(use)) {
                                userBB = userInst.getParent().getPredecessor(index);
                                curBB = (curBB == null) ? userBB : DT.findSharedParent(DT.getNode(curBB), DT.getNode(userBB)).BB;
                            }
                            index++;
                        }
                    } else {
                        curBB = (curBB == null) ? userBB : DT.findSharedParent(DT.getNode(curBB), DT.getNode(userBB)).BB;
                    }
                }
            }
            // upper:current lower:curBB
            BasicBlock minBB = curBB;
            int minLoopDepth = F.getLoopInfo().getLoopDepthForBB(minBB);
            while(curBB != I.getParent()) {
                curBB = DT.getNode(curBB).IDom.BB;
                int curLoopDepth = F.getLoopInfo().getLoopDepthForBB(curBB);
                if(curLoopDepth < minLoopDepth) {
                    minBB = curBB;
                    minLoopDepth = curLoopDepth;
                }
            }
            I.getInstNode().remove();
            minBB.getInstList().insertBeforeEnd(I.getInstNode());
            
            // 找当前BB最后位置
            for(Instruction inst :minBB.getInstList()) {
                if(!inst.getOp().equals(Ops.PHI)) {
                    if(inst.getOperandList().contains(I)) {
                        I.getInstNode().remove();
                        minBB.getInstList().insertBefore(I.getInstNode(), inst.getInstNode());
                    }
                }
            }
        }
    }

    public boolean scheduleAble(Instruction I) {
        return Instruction.isBinary(I.getOp()) || I.getOp().equals(Ops.Load) || I.getOp().equals(Ops.GetElementPtr)
            || (I.getOp().equals(Ops.Call) && ((CallInst) I).withoutGEP());
    }

    @Override
    public String getName() {
        return "GVNGCM";
    }
}
