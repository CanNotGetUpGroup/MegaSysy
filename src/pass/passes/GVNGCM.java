package pass.passes;

import ir.*;
import ir.Module;
import ir.Instruction.Ops;
import pass.ModulePass;
import ir.instructions.Instructions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import analysis.DominatorTree;
import util.IList;
import util.Pair;

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
        // functionGVN(F);
        functionGCM(F);
    }

    public void functionGVN(Function F) {
        valueTable.clear();
        BasicBlock entryBB = F.getEntryBB();
        Stack<BasicBlock> stk = new Stack<>();
        ArrayList<BasicBlock> reverse = new ArrayList<>();

        stk.push(entryBB);
        while(!stk.isEmpty()) {
            BasicBlock cur = stk.pop();
            reverse.add(cur);

        }

        for(BasicBlock BB : reverse) {
            basicBlockGVN(BB);
        }
    }

    public void basicBlockGVN(BasicBlock BB) {
        // 一些指令间优化？

        for(Instruction I :BB.getInstList()) {
            instructionGVN(I);
        }
    }

    public void instructionGVN(Instruction I) {

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
            if(I.isBinary() || I.getOp().equals(Ops.Load) || I.getOp().equals(Ops.GetElementPtr)) {
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
                                curBB = (curBB == null) ? userBB : lca(curBB, userBB);
                            }
                            index++;
                        }
                    } else {
                        curBB = (curBB == null) ? userBB : lca(curBB, userBB);
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
        return I.isBinary() || I.getOp().equals(Ops.Load) || I.getOp().equals(Ops.GetElementPtr)
            || (I.getOp().equals(Ops.Call) && ((CallInst) I).withoutGEP());
    }

    public BasicBlock lca(BasicBlock A, BasicBlock B) {
        //TODO : emmmm...这个性能吧
        Function F = A.getParent();
        DominatorTree DT = F.getDominatorTree();

        while(DT.getNode(A).level < DT.getNode(B).level) {
            B = DT.getNode(B).IDom.BB;
        }
        while(DT.getNode(B).level < DT.getNode(A).level) {
            A = DT.getNode(A).IDom.BB;
        }
        while(!A.equals(B)) {
            A = DT.getNode(A).IDom.BB;
            B = DT.getNode(B).IDom.BB;
        }
        return A;
    }

    @Override
    public String getName() {
        return "GVNGCM";
    }
}
