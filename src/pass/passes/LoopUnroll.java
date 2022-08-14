package pass.passes;

import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.*;
import ir.instructions.BinaryInstruction;
import ir.instructions.CmpInst;
import ir.instructions.Instructions.*;
import pass.FunctionPass;
import util.CloneMap;
import util.LoopUtils;
import util.Match;
import util.MyIRBuilder;

import javax.swing.plaf.ButtonUI;
import java.util.*;

public class LoopUnroll extends FunctionPass {
    private DominatorTree DT;
    private final int maxLoop = 5;
    private LoopInfo LI;
    private final MyIRBuilder builder = MyIRBuilder.getInstance();
    private Function F;

    @Override
    public void runOnFunction(Function F) {
        F.getLoopInfo().computeLoopInfo(F);
        LI = F.getLoopInfo();
        this.F = F;
        ArrayList<Loop> loops = LI.getTopLevelLoops();
        if (loops.isEmpty()) return;
        DT = F.getAndUpdateDominatorTree();
        new LCSSA().runOnFunction(F);
        Queue<Loop> WorkList = new LinkedList<>();

        for (Loop l : loops) {
            LoopUtils.addLoopToWorkList(l, WorkList);
        }
        while (!WorkList.isEmpty()) {
            Loop L = WorkList.poll();
            tryToUnrollLoop(L);
        }
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

    public void UnrollLoop(Loop L) {
        if (!L.isSimpleForLoop() || L.getBbList().size() > maxLoop || !canUnroll(L)) {
            return;
        }
        BasicBlock Header = L.getLoopHeader();
        BasicBlock LatchBB = L.getSingleLatchBlock();
        ArrayList<BasicBlock> ExitBBs = L.getExitBlocks();
        ArrayList<BasicBlock> OriBBs = L.getBbList();
        ArrayList<BasicBlock> ExitingBBs = L.getExitingBlocks();
        BasicBlock ExitingBB = ExitingBBs.size() == 1 ? ExitingBBs.get(0) : null;
        BasicBlock ExitBB = null;
        if (LatchBB == null || LatchBB != ExitingBB) {
            return;
        }
        BranchInst latchBr = (BranchInst) LatchBB.getTerminator();

        var step = L.getStep();
        var stepInst = L.getStepInst();
        if (step instanceof Constants.ConstantInt) {
            if (Math.abs(((Constants.ConstantInt) step).getVal()) > 100000000) {
                return;
            }
        }
        for (Instruction I : Header.getInstList()) {
            if (I instanceof GetElementPtrInst) {
                for (int i = 1; i < I.getNumOperands(); i++) {
                    if (I.getOperand(i) instanceof LoadInst) {
                        return;
                    }
                }
            }
        }
        if (OriBBs.contains(latchBr.getTrueBlock())) {
            ExitBB = latchBr.getFalseBlock();
        } else {
            ExitBB = latchBr.getTrueBlock();
        }
        if (ExitBB.getPredecessorsNum() > 2) return;

        HashMap<Value, Value> LastValMap = new HashMap<>();
        ArrayList<PHIInst> OriPhis = new ArrayList<>();
        ArrayList<BasicBlock> Headers = new ArrayList<>();
        ArrayList<BasicBlock> Latches = new ArrayList<>();
        Headers.add(Header);
        Latches.add(LatchBB);
        var LatchBr = LatchBB.getTerminator();
        var LatchCmpInst = L.getLatchCmpInst();
        int latchPredIndex = Header.getPredecessors().indexOf(LatchBB);

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

        int headerIdx = latchBr.getOperandList().indexOf(Header);
        latchBr.removeAllOperand();
        latchBr.getInstNode().remove();//只是从列表里删除

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
                    newPhi.removeAllOperand();
                    newPhi.getInstNode().remove();
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
            if (BB != nextHeader) {
                LastValMap.forEach((key, value) -> {
                    if (value == BB) {
                        for (BasicBlock pred : ((BasicBlock) key).getPredecessors()) {
                            if (!OriBBs.contains(pred)) {
                                System.out.println("oriBBs should contain pred");
                            }
                            BasicBlock newPred = (BasicBlock) LastValMap.get(pred);
                            newPred.getTerminator().addOperand(BB);
                        }
                    }
                });
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
        r = endIdx == 0 ? nextIndVarCondInst : L.getIndVar();
        CmpInst newICmpInst = (CmpInst) builder.createCmp(LatchCmpInst.getPredicate(), l, r);
        HashMap<Value, Value> iterValMap = new HashMap<>(LastValMap);

        // 保存 exit 中 phi 对应来自 latchBlock 的前驱
        // 后面设置 restBBLast 的 incomingVals 时使用这里缓存的原 incomingVals 作为查找 LastValMap 的索引
        ArrayList<Value> storeExitIncoming = new ArrayList<>();
        for (Instruction I : ExitBB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            storeExitIncoming.add(((PHIInst) I).getIncomingValueByBlock(LatchBB));
        }
        int exitLatchPredIndex = Header.getPredecessors().indexOf(LatchBB);
        var preHeader = Header.getPredecessors().get(1 - exitLatchPredIndex);

        // 修改 preHeader 进入循环的判断条件
        // while (i < n) => while (i + 1 < n)
        // preHeader 中的 icmp 的 i 设为 i + 1
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

        // 多出一次的迭代的收尾工作
        // original code : secondLatch -> exit
        // unroll code : secondLatch/preHeader -> exitIfBB, exitIfBB -> restBBHeader/exit, restBBLast -> exit
        // WARNING: this makes exitIfBB have a pred(preHeader) out of the loop

        // 构建 exitIfBB
        // 循环结束或在 preHeader 不进入循环时跳转到 exitIfBB，判断是否执行剩余一次迭代的计算，跳转到剩余一次迭代的基本块或原 exit
        var exitIfBB = builder.createBasicBlock(F);
        LI.addBBToLoop(exitIfBB, L.getParentLoop());
        // exitIfBB 中的指令：多个 Phi 承接来自循环或 preHeader 的 incomingVals，一条 Phi 表示迭代器，一个 icmp 指令，一个 Br，跳转到 exit 或 restBBHeader
        for (var I : Header.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            } else if (I == L.getIndVar()) {
                continue;
            }
            var copy = LoopUtils.copyInstruction(I);
            copy.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
            LoopUtils.remapInst(copy, LastValMap);
            LastValMap.put(iterValMap.get(I), copy);
        }
        var copyPhi = LoopUtils.copyInstruction(L.getIndVar());
        copyPhi.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
        var copyIcmp = LoopUtils.copyInstruction(LatchCmpInst);
        LoopUtils.remapInst(copyPhi, LastValMap);

        HashMap<Value, Value> exitPhiToExitIfPhiMap = new HashMap<>();
        for (var I : ExitBB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            var copyInst = LoopUtils.copyInstruction(I);
            copyInst.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
            var exitIncoming = ((PHIInst) I).getIncomingValueByBlock(LatchBB);
            copyInst.CoReplaceOperandByIndex(((PHIInst) I).getBlocks().indexOf(LatchBB), iterValMap.get(exitIncoming));
            exitPhiToExitIfPhiMap.put(I, copyInst);
        }

        var copyIndVarCondInst = LoopUtils.copyInstruction(L.getIndVar());
        LoopUtils.remapInst(copyIndVarCondInst, LastValMap);
        copyIndVarCondInst.CoReplaceOperandByIndex
                (L.getIndVar().getBlocks().indexOf(LatchBB),
                        LastValMap.get(L.getIndVarCondInst()));
        copyIndVarCondInst.getInstNode().insertIntoListEnd(exitIfBB.getInstList());
        copyIcmp.COReplaceOperand(L.getIndVarCondInst(), copyIndVarCondInst);
        copyIcmp.getInstNode().insertIntoListEnd(exitIfBB.getInstList());

        LastValMap.put(stepInst, copyPhi);
        LastValMap.put(LatchCmpInst, copyIcmp);

        ExitBB.replacePredecessorWith(LatchBB, exitIfBB);
        for (var I : ExitBB.getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            var phi = (PHIInst) I;
            var incomingVal = exitPhiToExitIfPhiMap.get(I);
            phi.CoReplaceOperandByIndex(phi.getBlocks().indexOf(exitIfBB), incomingVal);
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
                    newPhi.removeAllOperand();
                    newPhi.getInstNode().remove();
                }
            }
            LastValMap.put(bb, newBB);
            for (var key : valueMap.keySet()) {
                LastValMap.put(key, valueMap.get(key));
            }
            // TODO 多个出口时在这里更新 LCSSA 生成的 phi
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
            if (newBB != restBBHeader) {
                LastValMap.forEach((key, value) -> {
                    if (value == newBB) {
                        BasicBlock oldBB = (BasicBlock) key;
                        for (var pred : oldBB.getPredecessors()) {
                            if (!OriBBs.contains(pred)) {
                                System.out.println("should contain");
                            }
                            BasicBlock newPred = (BasicBlock) LastValMap.get(pred);
                            newPred.getTerminator().addOperand(newBB);
                        }
                    }
                });
            }
        }

        // 在复制完成多余的一次迭代后，header 才能更新 phi
        updatePhiNewIncoming(Header, LatchBB, L, iterValMap);

        // restBBs 连接到循环中
        builder.setInsertPoint(exitIfBB);
        builder.createCondBr(copyIcmp,restBBHeader,ExitBB);
        builder.setInsertPoint(restBBLast);
        builder.createBr(ExitBB);
        restBBHeader.getPredecessors().add(exitIfBB);

        // 假设 preHeader 只有 header 和 exit 两个后继，修改 exit 为 exitIfBB
        // exit 的前驱中 latchBlock 的位置替换成 exitIfBB
        preHeader.getTerminator().COReplaceOperand(ExitBB, exitIfBB);
//        nextLatch.getSuccessor_().add(exitIfBB);
//        int preHeaderIndex = Header.getPredecessors().indexOf(preHeader);
//        switch (preHeaderIndex) {
//            case 0 -> {
//                exitIfBB.getPredecessors().add(preHeader);
//                exitIfBB.getPredecessors().add(secondLatch);
//            }
//            case 1 -> {
//                exitIfBB.getPredecessors().add(secondLatch);
//                exitIfBB.getPredecessors().add(preHeader);
//            }
//        }

        // exit 的 predecessor 中，latchblock 的位置换成 exitIfBB（前面构造 exitIfBB 做了），加入 restBBLast 前驱，删去 preHeader
        // 维护 phi：exitIfBB 来源的 incomingVals 替换 latchBlock (前面构造 exitIfBB 时做的，防止 LastValMap 被更新) ，restBBLast 来源的
        // incomingVals 通过 LastValMap 查询 storeExitIncoming，preHeader 来源的 incomingVals 删去
        ((PHIInst)copyIndVarCondInst)
                .setOrAddIncomingValueByBlock(preStepInst,preHeader);

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
            phi.setOrAddIncomingValueByBlock(incomingVal,preHeader);
            cacheIndex++;
        }

        // link latchBlock and secondHeader
        builder.setInsertPoint(LatchBB);
        builder.createBr(nextHeader);

        // link secondLatch and header
        BasicBlock trueBB = headerIdx == 1 ? Header : exitIfBB;
        BasicBlock falseBB = trueBB == Header ? exitIfBB : Header;
        builder.setInsertPoint(nextLatch);
        builder.createCondBr(newICmpInst,trueBB,falseBB);
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
