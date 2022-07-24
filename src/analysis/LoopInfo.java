package analysis;

import ir.BasicBlock;
import ir.Function;
import ir.Loop;

import java.util.ArrayList;
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

    public ArrayList<Loop> getTopLevelLoops() {
        return topLevelLoops;
    }

    public ArrayList<Loop> getAllLoops() {
        return allLoops;
    }

    public Loop getLoopForBB(BasicBlock bb) {
        return bbLoopMap.get(bb);
    }

    public Integer getLoopDepthForBB(BasicBlock bb) {
        if (bbLoopMap.get(bb) == null) {
            return 0;
        }
        return bbLoopMap.get(bb).getLoopDepth();
    }

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
     * 
     * @param function LoopInfo的计算是以function为单位的
     */
    public void computeLoopInfo(Function function) {
        DominatorTree domInfo = new DominatorTree(function);
        Stack<BasicBlock> backEdgeTo = new Stack<>();
        // ! 后序遍历domtree domInfo.PostOrder是Dominate tree的后序遍历
        for (TreeNode DomNode : domInfo.PostOrder) {
            for (BasicBlock pred : DomNode.BB.getPredecessors()) {
                // 如果一个结点支配其前继，则将这个边识别为backedge
                if (domInfo.dominates(DomNode.BB, pred)) {
                    backEdgeTo.push(DomNode.BB);
                }
            }
            if (!backEdgeTo.isEmpty()) {
                // 初始化一个loop，以backedge的端点为loop header
                Loop loop = new Loop(DomNode.BB);
                // 构造循环体
                while (!backEdgeTo.isEmpty()) {
                    BasicBlock pred = backEdgeTo.pop();
                    Loop subloop = getLoopForBB(pred);
                    if (subloop == null) {
                        bbLoopMap.put(pred, loop);
                        if (pred == loop.getHeader()) {
                            continue;
                        }
                        
            }
        }
    }

}
