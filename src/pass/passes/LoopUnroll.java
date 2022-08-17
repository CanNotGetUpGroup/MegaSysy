package pass.passes;

import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.*;
import ir.Module;
import ir.instructions.BinaryInstruction;
import ir.instructions.CmpInst;
import ir.instructions.Instructions.*;
import pass.FunctionPass;
import util.*;

import javax.swing.plaf.ButtonUI;
import java.util.*;

public class LoopUnroll extends FunctionPass {
    private DominatorTree DT;
    private final int maxLoop = 5;
    private final int maxInstNum = 500;
    private LoopInfo LI;
    private final MyIRBuilder builder = MyIRBuilder.getInstance();
    private Function F;
    private final boolean constant;

    public LoopUnroll(boolean constant) {
        this.constant = constant;
    }

    @Override
    public void runOnFunction(Function F) {
        LCSSA lcssa=new LCSSA();
        GVNGCM gvngcm=new GVNGCM(true);
        SimplifyCFG simplifyCFG=new SimplifyCFG(false);
        boolean continueUnroll;

        do{
            continueUnroll=false;
            lcssa.runOnFunction(F);
//            simplifyCFG.runOnFunction(F);
            Module.getInstance().rename(F);

            F.getLoopInfo().computeLoopInfo(F);
            LI = F.getLoopInfo();
            this.F = F;
            ArrayList<Loop> loops = LI.getTopLevelLoops();
            if (loops.isEmpty()) return;
            DT = F.getAndUpdateDominatorTree();
            Queue<Loop> WorkList = new LinkedList<>();
            simplifyCFG.runOnFunction(F);

            for (Loop l : loops) {
                LoopUtils.addLoopToWorkList(l, WorkList);
            }
            while (!WorkList.isEmpty()) {
                Loop L = WorkList.poll();
                if(!constant){
                    tryToUnrollLoop(L);
                    simplifyCFG.runOnFunction(F);
                    Module.getInstance().rename(F);
                    gvngcm.functionGVNGCM(F);
                    Module.getInstance().rename(F);
                }else{//常数次展开
                    if(tryToConstantUnrollLoop(L)){//没有展开成功不要simplifyCFG，会破坏掉LCSSA
                        continueUnroll=true;
                        Module.getInstance().rename();
                        System.out.println("unroll "+L.getTripCount()+" loop");
                        simplifyCFG.runOnFunction(F);
                        Module.getInstance().rename(F);
                        gvngcm.functionGVNGCM(F);
                        Module.getInstance().rename(F);
                        break;
                    }
                }
            }
        }while (constant&&continueUnroll);
    }

    public void tryToUnrollLoop(Loop L) {
        if (!L.isSafeToCopy()) {
            return;
        }

        HashSet<BasicBlock> Visited = new HashSet<>();
        LoopUtils.rearrangeBBOrder(L, Visited);
        if (L.getTripCount() == null) {//非常数次展开
            UnrollLoop(L);
        }
    }

    public boolean tryToConstantUnrollLoop(Loop L) {
        if (L.getTripCount()==null||!L.isSafeToCopy()) {
            return false;
        }

        HashSet<BasicBlock> Visited = new HashSet<>();
        LoopUtils.rearrangeBBOrder(L, Visited);
        int tripCount = L.getTripCount(), instNum = 0;
        for (var BB : L.getBbList()) {
            for (var inst : BB.getInstList()) {
                if (!(inst instanceof PHIInst && BB != L.getLoopHeader())) {
                    instNum++;
                }
            }
        }
        if (tripCount == 1000000007 || tripCount == 0 || (tripCount!=300&&instNum * tripCount > maxInstNum)) {
            return false;
        }
        return ConstantUnroll(L);
    }

    BasicBlock Header, LatchBB, ExitingBB, ExitBB;
    ArrayList<BasicBlock> OriBBs, ExitingBBs;
    BranchInst latchBr;
    Value step;
    Instruction stepInst;
    CmpInst LatchCmpInst;
    HashMap<Value, Value> LastValMap;
    ArrayList<PHIInst> OriPhis;
    boolean headerIsTrueBB;
    int exitLatchPredIndex;

    public void UnrollLoop(Loop L) {
        if (!initInfo(L)) {
            return;
        }

        //复制循环
        BasicBlock nextHeader = null, nextLatch = null;
        ArrayList<BasicBlock> newBBs = new ArrayList<>();
        for (BasicBlock BB : OriBBs) {
            HashMap<Value, Value> valMap = new HashMap<>();
            BasicBlock newBB = LoopUtils.cloneBasicBlock(BB, valMap);
            if (BB == Header) {
                for (var phi : OriPhis) {
                    var newPhi = (PHIInst) valMap.get(phi);
                    var latchIncoming = newPhi.getIncomingValueByBlock(LatchBB);
                    valMap.put(phi, latchIncoming);
                    newPhi.remove();
                }
            }
            LastValMap.put(BB, newBB);
            valMap.forEach(LastValMap::put);
            LI.addBBToLoop(newBB, L);
            if (BB.equals(Header)) {
                nextHeader = newBB;
            }
            if (BB.equals(LatchBB)) {
                nextLatch = newBB;
            }
            newBBs.add(newBB);
        }

        for (BasicBlock BB : newBBs) {
            for (Instruction I : BB.getInstList()) {
                LoopUtils.remapInst(I, LastValMap);
            }
        }

        BinaryInstruction nextIndVarCondInst = (BinaryInstruction) LastValMap.get(L.getIndVarCondInst());
        BinaryInstruction nextStep = (BinaryInstruction) LastValMap.get(stepInst);
        int stepIdx = nextStep.getOperandList().indexOf(L.getStep());
        if (stepIdx == -1) {
            System.out.println("step idx shouldn't be -1");
        }
        Value l, r;
        l = stepIdx == 0 ? L.getStep() : nextIndVarCondInst;
        r = stepIdx == 0 ? nextIndVarCondInst : L.getStep();
        assert nextLatch != null;
        builder.setInsertPoint(nextLatch);
        BinaryInstruction nextStepIndVarCondInst;
        if (nextIndVarCondInst instanceof CmpInst)
            nextStepIndVarCondInst = (BinaryInstruction) builder.createICmp(((CmpInst) nextIndVarCondInst).getPredicate(), l, r);
        else
            nextStepIndVarCondInst = (BinaryInstruction) builder.createBinary(nextIndVarCondInst.getOp(), l, r);

        int endIdx = LatchCmpInst.getOperandList().indexOf(L.getIndVarEnd());
        if (endIdx == -1) {
            System.out.println("end idx shouldn't be -1");
        }
        l = endIdx == 0 ? L.getIndVarEnd() : nextStepIndVarCondInst;
        r = endIdx == 0 ? nextIndVarCondInst : L.getIndVarEnd();
        CmpInst newICmpInst = (CmpInst) builder.createCmp(LatchCmpInst.getPredicate(), l, r);
        HashMap<Value, Value> iterValMap = new HashMap<>(LastValMap);

        ArrayList<Value> storeExitIncoming = new ArrayList<>();
        for (Instruction I : ExitBB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            storeExitIncoming.add(((PHIInst) I).getIncomingValueByBlock(LatchBB));
        }
        var preHeader = Header.getPredecessors().get(0);

        // 修改 preHeader 进入循环的判断条件
        assert preHeader.getTerminator().getNumOperands() == 3;
        var preBrInst = preHeader.getTerminator();
        var preICmpInst = (CmpInst) (preBrInst.getOperandList().get(0));
        int preIndVarEndIndex = 0;
        var indVarEnd = L.getIndVarEnd();

        for (var op : preICmpInst.getOperandList()) {
            if (op == L.getIndVarEnd() || (op instanceof Constants.ConstantInt
                    && indVarEnd instanceof Constants.ConstantInt
                    && ((Constants.ConstantInt) op).getVal() == ((Constants.ConstantInt) indVarEnd).getVal())) {
                break;
            }
            preIndVarEndIndex++;
        }
        var preStepIndex = 1 - preIndVarEndIndex;
        var preStepInst = preICmpInst.getOperandList().get(preStepIndex);
        l = stepIdx == 0 ? L.getStep() : preStepInst;
        r = stepIdx == 0 ? preStepInst : L.getStep();
        var preStepIterOnce = BinaryInstruction.create(stepInst.getOp(), l, r, preICmpInst);
        l = preIndVarEndIndex == 0 ? L.getIndVarEnd() : preStepIterOnce;
        r = preIndVarEndIndex == 0 ? preStepIterOnce : L.getIndVarEnd();
        builder.setInsertPoint(preBrInst);
        var newPreIcmpInst = builder.createCmp(preICmpInst.getPredicate(), l, r);
        preBrInst.COReplaceOperand(preICmpInst, newPreIcmpInst);

        // preHeader->ExitBB和Latch->ExitBB替换为preHeader->exitIfBB和Latch->exitIfBB
        // 判断是否执行剩余一次迭代的计算，跳转到restBBs或ExitBB
        var exitIfBB = builder.createBasicBlock(F);
        LI.addBBToLoop(exitIfBB, L.getParentLoop());
        for (var I : Header.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            } else if (I == L.getIndVar()) {
                continue;
            }
            var copy = I.shallowCopy();
            copy.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
            LoopUtils.remapInst(copy, LastValMap);
            LastValMap.put(iterValMap.get(I), copy);
        }
        var copyPhi = L.getIndVar().shallowCopy();
        copyPhi.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
        var copyIcmp = (LatchCmpInst).shallowCopy();
        LoopUtils.remapInst(copyPhi, LastValMap);

        HashMap<Value, Value> exitPhiToExitIfPhiMap = new HashMap<>();
        for (var I : ExitBB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            var copyInst = (I).shallowCopy();
            copyInst.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
            var exitIncoming = ((PHIInst) I).getIncomingValueByBlock(LatchBB);
            copyInst.CoReplaceOperandByIndex(((PHIInst) I).getBlocks().indexOf(LatchBB), iterValMap.get(exitIncoming));
            exitPhiToExitIfPhiMap.put(I, copyInst);
        }

        var copyIndVarCondInst = (L.getIndVar()).shallowCopy();
        LoopUtils.remapInst(copyIndVarCondInst, LastValMap);
        copyIndVarCondInst.CoReplaceOperandByIndex
                (L.getIndVar().getBlocks().indexOf(LatchBB),
                        LastValMap.get(L.getIndVarCondInst()));
        copyIndVarCondInst.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
        copyIcmp.COReplaceOperand(L.getIndVarCondInst(), copyIndVarCondInst);
        copyIcmp.getInstNode().insertIntoListEnd(exitIfBB.getInstList());

        LastValMap.put(stepInst, copyPhi);
        LastValMap.put(LatchCmpInst, copyIcmp);

        for (var I : ExitBB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            var phi = (PHIInst) I;
            var incomingVal = exitPhiToExitIfPhiMap.get(I);
            phi.replaceIncomingByBlock(LatchBB, exitIfBB, incomingVal);
        }
        // 复制多余的一次迭代
        ArrayList<BasicBlock> restBBs = new ArrayList<>();
        BasicBlock restBBHeader = null, restBBLast = null;
        for (var bb : OriBBs) {
            HashMap<Value, Value> valueMap = new HashMap<>();
            BasicBlock newBB = LoopUtils.cloneBasicBlock(bb, valueMap);
            if (bb == Header) {
                for (var phi : OriPhis) {
                    var newPhi = (PHIInst) valueMap.get(phi);
                    var latchIncomingVal = newPhi.getIncomingValueByBlock(LatchBB);
                    if (latchIncomingVal instanceof Instruction &&
                            (OriBBs.contains(((Instruction) latchIncomingVal).getParent()))) {
                        latchIncomingVal = LastValMap.get(latchIncomingVal);
                    }
                    valueMap.put(phi, latchIncomingVal);
                    newPhi.remove();
                }
            }
            LastValMap.put(bb, newBB);
            for (var key : valueMap.keySet()) {
                LastValMap.put(key, valueMap.get(key));
            }
            if (bb == Header) {
                restBBHeader = newBB;
            }
            if (bb == LatchBB) {
                restBBLast = newBB;
            }
            LI.addBBToLoop(newBB, L.getParentLoop());
            restBBs.add(newBB);
        }
        assert restBBHeader != null && restBBLast != null;
        for (var newBB : restBBs) {
            newBB.getInstList().forEach(inst -> {
                LoopUtils.remapInst(inst, LastValMap);
            });
        }

        updatePhiNewIncoming(Header, LatchBB, L, iterValMap);

        // exitIfBB->restBBs
        builder.setInsertPoint(exitIfBB);
        builder.createCondBr(copyIcmp, restBBHeader, ExitBB);
        exitIfBB.getBbNode().insertBefore(ExitBB.getBbNode());
        builder.setInsertPoint(restBBLast);
        builder.createBr(ExitBB);
        IListIterator<BasicBlock, Function> ExitIt = F.getBbList().iterator(ExitBB);
        F.getBbList().splice(ExitIt, restBBHeader.getBbNode(), restBBLast.getBbNode().getNext());

        // 假设 preHeader 只有 Header 和 Exit 两个后继
        // preHeader->ExitBB替换为preHeader->exitIfBB->ExitBB
        preHeader.getTerminator().COReplaceOperand(ExitBB, exitIfBB);

        // LatchBB->ExitBB替换为exitIfBB->ExitBB和exitIfBB->restBB->ExitBB
        //
        ((PHIInst) copyIndVarCondInst)
                .setOrAddIncomingValueByBlock(preStepInst, preHeader);

        int cacheIndex = 0;
        for (var inst : ExitBB.getInstList()) {
            if (!(inst instanceof PHIInst)) {
                break;
            }
            var phi = (PHIInst) inst;
            var incomingVal = storeExitIncoming.get(cacheIndex);
            if (incomingVal instanceof Instruction &&
                    (OriBBs.contains(((Instruction) incomingVal).getParent()))) {
                incomingVal = LastValMap.get(incomingVal);
            }
            phi.replaceIncomingByBlock(preHeader, restBBLast, incomingVal);
            cacheIndex++;
        }

        // link latchBlock and secondHeader
        builder.setInsertPoint(LatchBB);
        builder.createBr(nextHeader);
        assert nextHeader != null;
        nextHeader.getBbNode().insertAfter(LatchBB.getBbNode());

        // link secondLatch and header
        BasicBlock trueBB = headerIsTrueBB ? Header : exitIfBB;
        BasicBlock falseBB = headerIsTrueBB ? exitIfBB : Header;
        builder.setInsertPoint(nextLatch);
        builder.createCondBr(newICmpInst, trueBB, falseBB);
        for(Instruction I:Header.getInstList()){
            if(!(I instanceof PHIInst)){
                break;
            }
            ((PHIInst) I).replaceIncomingBlock(LatchBB,nextLatch);
        }
        System.out.println("successfully unroll simple loop");
    }

    private boolean initInfo(Loop L) {
        if (!L.isSimpleForLoop()) {
            return false;
        }

        if (!constant && (L.getBbList().size() > maxLoop || !canUnroll(L))) {
            return false;
        }
        //Init
        Header = L.getLoopHeader();
        LatchBB = L.getSingleLatchBlock();
        OriBBs = new ArrayList<>(L.getBbList());
        ExitingBBs = L.getExitingBlocks();
        ExitingBB = ExitingBBs.size() == 1 ? ExitingBBs.get(0) : null;
        ExitBB = null;
        if (!constant&&(LatchBB == null || LatchBB != ExitingBB)) {
            return false;
        }
        latchBr = (BranchInst) LatchBB.getTerminator();

        step = L.getStep();
        stepInst = L.getStepInst();
        if (step instanceof Constants.ConstantInt) {
            if (Math.abs(((Constants.ConstantInt) step).getVal()) > 100000000) {
                return false;
            }
        }
        for (Instruction I : Header.getInstList()) {
            if (I instanceof GetElementPtrInst) {
                for (int i = 1; i < I.getNumOperands(); i++) {
                    if (I.getOperand(i) instanceof LoadInst) {
                        return false;
                    }
                }
            }
        }
        if (OriBBs.contains(latchBr.getTrueBlock())) {
            ExitBB = latchBr.getFalseBlock();
        } else {
            ExitBB = latchBr.getTrueBlock();
        }
        if (ExitBB.getPredecessorsNum() > 2) return false;

        LastValMap = new HashMap<>();
        OriPhis = new ArrayList<>();
        LatchCmpInst = L.getLatchCmpInst();

        for (Instruction I : Header.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            OriPhis.add((PHIInst) I);
            var latchIncoming = ((PHIInst) I).getIncomingValueByBlock(LatchBB);
            if (latchIncoming instanceof Instruction && OriBBs.contains(((Instruction) latchIncoming).getParent())) {
                LastValMap.put(latchIncoming, latchIncoming);
            }
        }


        headerIsTrueBB = latchBr.getTrueBlock().equals(Header);
        exitLatchPredIndex = Header.getPredecessors().indexOf(LatchBB);
        latchBr.remove();//记得维护PHIs
//        LatchBB.getPHIs().clear();
        return true;
    }

    private boolean ConstantUnroll(Loop L) {
        if (!initInfo(L)) {
            return false;
        }

        int tripCount = L.getTripCount();
        ArrayList<BasicBlock> headers = new ArrayList<>();
        headers.add(Header);
        ArrayList<BasicBlock> latches = new ArrayList<>();
        latches.add(LatchBB);

        for (int i = 1; i < L.getTripCount(); i++) {
            //复制循环
            ArrayList<BasicBlock> newBBs = new ArrayList<>();
            for (BasicBlock BB : OriBBs) {
                HashMap<Value, Value> valMap = new HashMap<>();
                BasicBlock newBB = LoopUtils.cloneBasicBlock(BB, valMap);
                if (BB == Header) {
                    for (var phi : OriPhis) {
                        var newPhi = (PHIInst) valMap.get(phi);
                        var latchIncoming = newPhi.getIncomingValueByBlock(LatchBB);
                        if (latchIncoming instanceof Instruction &&
                                (OriBBs.contains(((Instruction) latchIncoming).getParent())) && i > 1) {
                            latchIncoming = LastValMap.get(latchIncoming);
                        }
                        valMap.put(phi, latchIncoming);
                        newPhi.remove();
                    }
                }
                LastValMap.put(BB, newBB);
                valMap.forEach(LastValMap::put);
                LI.addBBToLoop(newBB, L);
                if (BB.equals(Header)) {
                    headers.add(newBB);
                }
                if (BB.equals(LatchBB)) {
                    latches.add(newBB);
                }
                newBBs.add(newBB);
            }

            for (BasicBlock BB : newBBs) {
                for (Instruction I : BB.getInstList()) {
                    LoopUtils.remapInst(I, LastValMap);
                }
            }

            Module.getInstance().rename(F);
        }

        HashMap<Value,Value> exitIncoming=new HashMap<>();
        //储存一下ExitBB的incomingVal
        if (tripCount > 1) {
            for (var exitBB : L.getExitBlocks()) {
                for (var inst : exitBB.getInstList()) {
                    if (!(inst instanceof PHIInst)) {
                        break;
                    }
                    var phi = (PHIInst) inst;
                    var latchIndex = phi.getBlocks().indexOf(LatchBB);
                    if(latchIndex==-1) continue;
                    var incomingVal = phi.getIncomingValues().get(latchIndex);
                    if (incomingVal instanceof Instruction && (L.getBbList()
                            .contains(((Instruction) incomingVal).getParent()))) {
                        incomingVal = LastValMap.get(incomingVal);
                    }
                    exitIncoming.put(phi,incomingVal);
                }
            }
        }

        var preHeader = Header.getPredecessors().get(0);

        for (var phi : OriPhis) {
            phi.replaceAllUsesWith(phi.getIncomingValueByBlock(preHeader));
            phi.remove();
        }
        for (int i = 1; i < latches.size(); i++) {
            var succ = headers.get(i);
            var pred = latches.get(i - 1);
            builder.setInsertPoint(pred);
            builder.createBr(succ);
            succ.getBbNode().insertBefore(pred.getBbNode());
        }
        var last = latches.get(latches.size() - 1);
        builder.setInsertPoint(last);
        builder.createBr(ExitBB);

        // 更新 LCSSA phi
        if (tripCount > 1) {
            for (var exitBB : L.getExitBlocks()) {
                for (var inst : exitBB.getInstList()) {
                    if (!(inst instanceof PHIInst)) {
                        break;
                    }
                    var phi = (PHIInst) inst;
                    var incomingVal = exitIncoming.get(phi);
                    if(incomingVal==null) continue;
                    phi.replaceIncomingByBlock(LatchBB,last, incomingVal);
                }
            }
        }

        LI.removeLoop(L);
        return true;
    }

    private void updatePhiNewIncoming(BasicBlock bb, BasicBlock pred, Loop loop,
                                      HashMap<Value, Value> lastValueMap) {
        for (var inst : bb.getInstList()) {
            if (!(inst instanceof PHIInst)) {
                break;
            }
            var phi = (PHIInst) inst;
            var incomingVal = phi.getIncomingValueByBlock(pred);
            if (incomingVal instanceof Instruction && (loop.getBbList().contains(((Instruction) incomingVal).getParent()))) {
                incomingVal = lastValueMap.get(incomingVal);
            }
            phi.CoReplaceOperandByIndex(phi.getBlocks().indexOf(pred), incomingVal);
        }
    }

    private boolean canUnroll(Loop loop) {
        return loop.getIndVar() != null && (loop.getStepInst().getOp().equals(Instruction.Ops.Sub) ||
                loop.getStepInst().getOp().equals(Instruction.Ops.Add)) &&
                (loop.getLatchCmpInst().getPredicate().compareTo(CmpInst.Predicate.ICMP_SGT) >= 0 &&
                        loop.getLatchCmpInst().getPredicate().compareTo(CmpInst.Predicate.ICMP_SLE) <= 0);
    }

    @Override
    public String getName() {
        return "Loop Unroll";
    }
}
