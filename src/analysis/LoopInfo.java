package analysis;

import ir.BasicBlock;
import ir.Function;
import ir.Loop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;

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
     * 分析算法会按dom-tree的后序序列进行遍历 这样可以先找到最内层的循环（后序，逆后序）
     * 每遍历一个node就会看其对应的block是否支配他的某个前继block
     * 如果是，说明这个block是loop header，而它和这个前继block之间的边就是back edge。
     * 一旦找到这样一对block后，就可以从个前继block开始沿着reverse cfg来找到循环体中的所有结点。
     * https://blog.csdn.net/yeshahayes/article/details/97233940
     * 
     * @param function LoopInfo的计算是以function为单位的
     */
    public void computeLoopInfo(Function function) {
        DominatorTree domInfo = new DominatorTree(function);
        Stack<BasicBlock> backNode = new Stack<>();
        // ! 后序遍历domtree domInfo.PostOrder是Dominate tree的后序遍历
        for (TreeNode DomNode : domInfo.PostOrder) {
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
    }

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
        }

    }

}