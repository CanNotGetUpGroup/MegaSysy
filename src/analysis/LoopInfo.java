package analysis;

import ir.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;
import java.util.prefs.BackingStoreException;

import ir.instructions.BinaryInstruction;
import ir.instructions.CmpInst;
import ir.instructions.Instructions;
import org.antlr.v4.tool.GrammarParserInterpreter.BailButConsumeErrorStrategy;

import analysis.DominatorTree.TreeNode;

/**
 * Algorithm: Testing flow graph reducibility
 * 根据DominatorTree生成的LoopInfo对象，不考虑不可规约的loop（不可规约循环的循环头不支配循环体）
 * 参考资料：https://blog.csdn.net/yeshahayes/article/details/97233940
 */
public class LoopInfo {

    private HashMap<BasicBlock, Loop> bbLoopMap; // map between basic block and the most inner loop
    private ArrayList<Loop> topLevelLoops;
    private ArrayList<Loop> allLoops;

    /**
     * 构造函数 new三个容器
     */
    public LoopInfo() {
        this.bbLoopMap = new HashMap<>();
        this.topLevelLoops = new ArrayList<>();
        this.allLoops = new ArrayList<>();
    }

    /**
     * 获取function顶层的Loop
     * 
     * @return
     */
    public ArrayList<Loop> getTopLevelLoops() {
        return topLevelLoops;
    }

    /**
     * 获取function所有的Loop
     * 
     * @return
     */
    public ArrayList<Loop> getAllLoops() {
        return allLoops;
    }

    /**
     * 获取basic block所在的loop
     * 
     * @param bb
     * @return
     */
    public Loop getLoopForBB(BasicBlock bb) {
        return bbLoopMap.get(bb);
    }

    /**
     * 获取basic block所在的loop的深度，深度从0开始计算
     * not in loop iff depth=0
     * 
     * @param bb
     * @return
     */
    public Integer getLoopDepthForBB(BasicBlock bb) {
        if (bbLoopMap.get(bb) == null) {
            return 0;
        }
        return bbLoopMap.get(bb).getLoopDepth();
    }

    /**
     * 判断一个BasicBlock是否是循环的header
     * 
     * @param bb
     * @return
     */
    public Boolean isLoopHeader(BasicBlock bb) {
        var loop = bbLoopMap.get(bb);
        if (loop == null) {
            return false;
        }
        return loop.getBlockHeader() == bb;
    }

    /**
     * 判断一个BasicBlock是否是循环的preheader
     *
     * @param bb
     * @return
     */
    public Boolean isLoopPreHeader(BasicBlock bb) {
        for (var loop : allLoops) {
            if (loop.getPreHeader() == bb) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        bbLoopMap.clear();
        allLoops.clear();
        topLevelLoops.clear();
    }

    /**
     * 分析算法会按dom-tree的后序序列进行遍历 这样可以先找到最内层的循环（后序，逆后序）
     * 每遍历一个node就会看其对应的block是否支配他的某个前继block
     * 如果是，说明这个block是loop header，而它和这个前继block之间的边就是back edge。
     * 一旦找到这样一对block后，就可以从个前继block开始沿着reverse cfg来找到循环体中的所有结点。
     * https://blog.csdn.net/yeshahayes/article/details/97233940
     * 
     * @param function LoopInfo的计算是以function为单位的
     */
    public void computeLoopInfo(Function function) {
        clear();
        DominatorTree domInfo = new DominatorTree(function);
        Stack<BasicBlock> backNode = new Stack<>();
        // ! 后序遍历domtree domInfo.PostOrder是Dominate tree的后序遍历
        for (TreeNode DomNode : domInfo.getDTPostOrder()) {
            for (BasicBlock pred : DomNode.BB.getPredecessors()) {
                // 如果一个结点支配其前继，则将这个边识别为backedge
                if (domInfo.dominates(DomNode.BB, pred)) {
                    backNode.push(pred);
                }
            }
            if (!backNode.isEmpty()) {
                // 初始化一个loop，以backedge的端点为loop header
                Loop loop = new Loop(DomNode.BB);
                // 构造循环体
                while (!backNode.isEmpty()) {
                    BasicBlock pred = backNode.pop();
                    Loop subloop = getLoopForBB(pred); // 查询pred是否已经在某个subLoop中了
                    if (subloop == null) {
                        bbLoopMap.put(pred, loop); // 添加Block和Loop的映射关系
                        if (pred == loop.getBlockHeader()) { // 如果pred是loop.bblist的第一个，说明pred就是初始化loop时添加的header
                            continue;
                        }
                        for (BasicBlock predPred : pred.getPredecessors()) { // 用CFG图遍历Loop
                            backNode.push(predPred);
                        }
                    } else {
                        while (subloop.getParentLoop() != null) {
                            subloop = subloop.getParentLoop();
                        }
                        if (subloop == loop) {
                            continue;
                        }
                        subloop.setParentLoop(loop);
                        for (BasicBlock subHeaderPred : subloop.getBlockHeader().getPredecessors()) {
                            Loop tmp = bbLoopMap.get(subHeaderPred);
                            if (tmp == null || !tmp.equals(subloop)) {
                                backNode.push(subHeaderPred);
                            }
                        }
                    }
                }
            }
        }
        HashMap<BasicBlock, Boolean> travelMap = new HashMap<>();
        InitTravelMap(travelMap, function.getEntryBB());
        PopulateLoopsDFS(travelMap, function.getEntryBB());
        computeAllLoops();
        for(Loop loop:allLoops) loop.clear();
        computeExitingBlocks();
        computeExitBlocks();
        computeLoopPreheader();
        computeLatchBlocks();
        computeIndVarInfo();
    }

    /**
     * 初始化CFG的TravelMap，false代表没有遍历过
     * 
     * @param travelMap
     * @param bb
     */
    public void InitTravelMap(HashMap<BasicBlock, Boolean> travelMap, BasicBlock bb) {
        if (travelMap.containsKey(bb)) {
            return;
        } else {
            travelMap.put(bb, false);
            for (BasicBlock succ : bb.getSuccessors()) {
                InitTravelMap(travelMap, succ);
            }
        }
    }

    /**
     * DFS遍历CFG 维护loopInfo中的subloop、TopLevlLoops、bblist集合
     * 
     * @param travelMap
     * @param bb
     */
    public void PopulateLoopsDFS(HashMap<BasicBlock, Boolean> travelMap, BasicBlock bb) {
        if (travelMap.get(bb)) {
            return;
        }
        travelMap.replace(bb, true);

        for (BasicBlock succ : bb.getSuccessors()) {
            PopulateLoopsDFS(travelMap, succ);
        }

        Loop subLoop = getLoopForBB(bb);
        if (subLoop != null && bb == subLoop.getLoopHeader()) {
            if (subLoop.getParentLoop() != null) { // 维护loop.subloops集合
                subLoop.getParentLoop().getSubLoops().add(subLoop);
            } else { // 维护TopLevlLoops集合
                topLevelLoops.add(subLoop);
            }

            // 反转 subLoop.getBlocks[1, size - 1]
            // "For convenience, Blocks and Subloops are inserted in postorder. Reverse
            // the lists, except for the loop header, which is always at the beginning."
            Collections.reverse(subLoop.getBbList());
            subLoop.getBbList().add(0, bb);
            subLoop.getBbList().remove(subLoop.getBbList().size() - 1);
            Collections.reverse(subLoop.getSubLoops());
            subLoop = subLoop.getParentLoop(); // 实现：如果是headBlock，已经在初始化时放到parent loop中了，直接从parent-parentloop开始放置
        }

        // 在每个祖先循环中加入当前basicblock
        for (; subLoop != null; subLoop = subLoop.getParentLoop())
            subLoop.getBbList().add(bb); // 维护loopp.bblist
    }

    /**
     * 维护loopInfo.allloop集合
     */
    private void computeAllLoops() {
        Stack<Loop> loopStack = new Stack<>();
        allLoops.addAll(topLevelLoops);
        loopStack.addAll(topLevelLoops);
        while (!loopStack.isEmpty()) {
            var loop = loopStack.pop();
            if (!loop.getSubLoops().isEmpty()) {
                allLoops.addAll(loop.getSubLoops());
                loopStack.addAll(loop.getSubLoops());
            }
        }
    }

    /**
     * 维护loop.exitingBlocks集合
     * algorithm: 如果有后继不再循环内，则它就是exiting block
     * 此算法计算的exitingBlock并不是规范化的
     */
    private void computeExitingBlocks() {
        for (var loop : allLoops) {
            for (var bb : loop.getBbList()) {
                for (var succ : bb.getSuccessors()) {
                    if (!loop.getBbList().contains(succ)) {
                        loop.getExitingBlocks().add(bb);
                    }
                }
            }
        }
    }

    /**
     * 维护loop.exitBlocks集合
     * algorithm: 如果有后继不在循环内，后继就是exit block
     * 此算法计算的exitBlock并不是规范化的
     */
    private void computeExitBlocks() {
        for (var loop : allLoops) {
            for (var bb : loop.getBbList()) {
                for (var succ : bb.getSuccessors()) {
                    if (!loop.getBbList().contains(succ)) {
                        loop.getExitBlocks().add(succ);
                    }
                }
            }
        }
    }

    /**
     * 获取循环头的唯一前继
     */
    private BasicBlock getLoopPredecessor(Loop loop) {
        BasicBlock loopHeader = loop.getLoopHeader();
        BasicBlock ret = null;
        for (var pred : loopHeader.getPredecessors()) {
            if (!loop.getBbList().contains(pred)) {
                if (ret != null && ret != pred) {
//                    System.out.println("Error: loop header has multiple predecessors");
                    return null;
                }
                ret = pred;
            }
        }
        return ret;
    }

    /**
     * 判断循环头的唯一前继是否以循环头为唯一后继，
     * 同时判断该前继是否以循环头为唯一后继。
     * 还需要确定能否将代码提升到这个前继中。
     * 
     * @param loop
     * @return
     */
    private BasicBlock getLoopPreheader(Loop loop) {
        // 获取唯一循环外前继
        BasicBlock loopPreheader = getLoopPredecessor(loop);
        if (loopPreheader == null) {
            return null;
        }
        // 判断循环头是否也是该前继的唯一后继
        int size = 0;
        for (var succ : loopPreheader.getSuccessors()) {
            if (loop.getLoopHeader() == succ) {
                size++;
            }
        }
        if (size != loopPreheader.getSuccessors().size()) { // size不等说明循环头不是preheader的唯一后继
            return null;
        }
        // 判断能否将代码提升到这个前继
        if (!loopPreheader.isLegalToHoistInto()) {
            return null;
        }
        return loopPreheader;
    }

    /**
     * 维护所有循环的looppreheader
     */
    private void computeLoopPreheader() {
        for (var loop : allLoops) {
            loop.setLoopPrehead(getLoopPreheader(loop));
        }
    }

    /**
     * 计算循环loop的唯一latchblock
     * 
     * @param loop
     * @return 如果不存在latchblock或者存在多个latchblock，则返回null
     */
    private BasicBlock getLoopLatchBlock(Loop loop) {
        BasicBlock loopHeader = loop.getLoopHeader();
        BasicBlock ret = null;
        for (var pred : loopHeader.getPredecessors()) {
            if (loop.getBbList().contains(pred)) {
                if (ret != null) {
//                    System.out.println("Error: loop has multiple latchblocks");
                    return null;
                }
                ret = pred;
            }
        }
        return ret;
    }

    /**
     * 维护所有循环的latchblock
     * 注意，这里纳入计算的latchblock是规范化的，也就是说一个循环只有一个latchBlock 否则认为他的latchBlock为null
     */
    private void computeLatchBlocks() {
        for (Loop loop : allLoops) {
//            BasicBlock latchBlock = getLoopLatchBlock(loop);
//            if (latchBlock != null) {
//                loop.getLatchBlocks().add(latchBlock);
//            }
            for(BasicBlock Pred:loop.getLoopHeader().getPredecessors()){
                if(loop.getBbList().contains(Pred)){
                    loop.getLatchBlocks().add(Pred);
                }
            }
        }
    }

    private void computeIndVarInfo(){
        for(Loop L:allLoops){
            var latchCmp=L.getLatchCmpInst();
            if(!L.isSimpleForLoop()||latchCmp==null){
                return;
            }
//            System.out.println(L.getSingleLatchBlock());
//            System.out.println(latchCmp);
            getIndVariable(L);
            if(L.getIndVarCondInst()==null
                    ||(!(L.getIndVarCondInst() instanceof BinaryInstruction))){
                return;
            }
            var indVarCondInst = L.getIndVarCondInst();
            Value compareBias = null;
            for (var op : indVarCondInst.getOperandList()) {
                if (op instanceof Instructions.PHIInst) {
                    L.setIndVar((Instructions.PHIInst) op);
                } else {
                    compareBias = op;
                }
            }
            assert compareBias != null;
            if (L.getIndVar() == null) {
                return;
            }

            getStepInst(L);
            getTripCount(L,compareBias);
        }
    }

    private void getIndVariable(Loop loop){
        var latchCmp=loop.getLatchCmpInst();
        int idx = 0,end=0;
        for (var i = 0; i <= 1; i++) {
            var op = latchCmp.getOperand(i);
            if (op instanceof Instruction) {
                Instruction opInst = (Instruction) op;
                if (!getLoopDepthForBB(opInst.getParent()).equals(getLoopDepthForBB(latchCmp.getParent()))) {
                    idx=1-i;
                    end=i;
                } else {
                    idx=i;
                    end=1-i;
                }
            } else {
                idx=1-i;
                end=i;
            }
        }
        loop.setIndVarCondInst((Instruction) latchCmp.getOperand(idx));
        loop.setIndVarEnd(latchCmp.getOperand(end));
    }

    private void getStepInst(Loop L){
        var indVar = L.getIndVar();
        int indVarDepth = this.getLoopDepthForBB(indVar.getParent());
        for (var incomingVal : indVar.getIncomingValues()) {
            if (incomingVal instanceof Instruction) {
                Instruction inst = (Instruction) incomingVal;
                int incomingValDepth = this.getLoopDepthForBB(inst.getParent());
                if (indVarDepth != incomingValDepth) {
                    L.setIndVarInit(incomingVal);
                } else {
                    L.setStepInst((Instruction) incomingVal);
                }
            } else {
                L.setIndVarInit(incomingVal);
            }
        }

        var stepInst = L.getStepInst();
        if (stepInst == null) {
            return;
        }
        for (var op : stepInst.getOperandList()) {
            if (op != indVar) {
                L.setStep(op);
            }
        }
    }

    private void getTripCount(Loop L,Value compareBias){
        var stepInst=L.getStepInst();
        var latchCmp=L.getLatchCmpInst();
        if (stepInst.getOp().equals(Instruction.Ops.Add) &&
                L.getStep() instanceof Constants.ConstantInt && L.getIndVarInit() instanceof Constants.ConstantInt &&
                L.getIndVarEnd() instanceof Constants.ConstantInt && compareBias instanceof Constants.ConstantInt) {
            int init = ((Constants.ConstantInt) L.getIndVarInit()).getVal();
            int end = ((Constants.ConstantInt) L.getIndVarEnd()).getVal();
            int step = ((Constants.ConstantInt) L.getStep()).getVal();
            int bias = ((Constants.ConstantInt) compareBias).getVal();
            int tripCount = 1000000007;

            switch (latchCmp.getPredicate()) {
                case ICMP_SLT -> {
                    if (step > 0) {
                        tripCount = init < end ? (int)Math.ceil((double)(end - init)/step):0;
                    }
                }
                case ICMP_SGT -> {
                    if (step < 0) {
                        tripCount = init > end ? (int)Math.ceil((double)(init - end)/step):0;
                    }
                }
                case ICMP_SLE -> {
                    if (step > 0) {
                        tripCount = init <= end ? (int)Math.ceil((double)(end - init + 1)/step):0;
                    }
                }
                case ICMP_SGE -> {
                    if (step < 0) {
                        tripCount = init >= end ? (int)Math.ceil((double)(init - end + 1)/step):0;
                    }
                }
                case ICMP_NE -> {
                    if (end - init == 0) {
                        tripCount = 0;
                    } else if (step * (end - init) > 0 && (end - init) % step == 0) {
                        tripCount = (end - init) / step;
                    }
                }
            }

            tripCount -= (bias - step);

            L.setTripCount(tripCount);
        }
    }

    /**
     * getter
     * 
     * @return bbloopMap
     */
    public HashMap<BasicBlock, Loop> getBbLoopMap() {
        return bbLoopMap;
    }

    /**
     * 向loop中加入一个基本块，并且更新loop及其亲代loop的bbList
     * 
     * @param bb
     * @param loop
     */
    public void addBBToLoop(BasicBlock bb, Loop loop) {
        if (loop == null) {
            return;
        }

        this.bbLoopMap.put(bb, loop);
        loop.addBlock(bb);
    }

    /**
     * 删除一个基本块，并且更新loop及其亲代loop的bbList
     * 
     * @param bb
     */
    public void removeBBFromAllLoops(BasicBlock bb) {
        var loop = getLoopForBB(bb);
        while (loop != null) {
            loop.removeBlock(bb);
            loop = loop.getParentLoop();
        }
        this.bbLoopMap.remove(bb);
    }

    /**
     * 删除一个顶层循环 util for removeLoop
     * 
     * @param loop
     */
    public void removeTopLevelLoop(Loop loop) {
        this.topLevelLoops.remove(loop);
    }

    /**
     * 添加一个顶层循环，util for removeLoop
     * 
     * @param loop
     */
    public void addTopLevelLoop(Loop loop) {
        this.topLevelLoops.add(loop);
    }

    /**
     * 从循环嵌套结构中删除一个循环
     * 
     * @param loop
     */
    public void removeLoop(Loop loop) {
        ArrayList<BasicBlock> loopBlocks = new ArrayList<>();
        loopBlocks.addAll(loop.getBbList());
        if (loop.getParentLoop() != null) {
            var parentLoop = loop.getParentLoop();
            for (var bb : loopBlocks) {
                if (this.getLoopForBB(bb) == loop) {
                    this.getBbLoopMap().put(bb, parentLoop); // todo ？？？？
                }
            }
            // 从parentLoop中删除loop
            parentLoop.removeSubLoop(loop);
            // 将loop的子loop设置为parentLoop的子loop
            while (loop.getSubLoops().size() != 0) {
                var subLoop = loop.getSubLoops().get(0);
                loop.removeSubLoop(subLoop);
                parentLoop.addSubLoop(subLoop);
            }
        } else {
            for (var bb : loopBlocks) {
                if (this.getLoopForBB(bb) == loop) {
                    // bb 在最外层循环里了
                    this.removeBBFromAllLoops(bb);
                }
            }

            this.removeTopLevelLoop(loop);
            while (loop.getSubLoops().size() != 0) {
                var subLoop = loop.getSubLoops().get(0);
                loop.removeSubLoop(subLoop);
                this.addTopLevelLoop(subLoop);
            }
        }
    }
}