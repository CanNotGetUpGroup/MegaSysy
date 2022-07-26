package pass.passes;

import analysis.LoopInfo;
import ir.Function;
import ir.Loop;
import pass.FunctionPass;

/**
 * LoopSimplify 循环简化
 * 
 * @content 循环简化形式是一种规范的形式，它使多项分析和转换更简单，更有效。该Pass执行后循环将具有：
 *          --: 一个Preheader
 *          --: 单个的返回边（这意味着只有一个闩锁）。
 *          --: 专用出口。即该循环的任何出口块都没有位于循环外部的前驱。这意味着所有出口块都由循环头控制。
 * @precondition 需要在LoopInfoUpdate执行后进行
 * 
 * @algorithm 先判断该loop是否是simple-for，如果是则不简化，否则进入下述简化流程：
 *            step1.在循环的header basicblock前插入一个empty basicblock
 *            它的前继是header basicblock的前继集，它的后继是header basicblock
 *            step2.在循环的header basicblock前插入一个empty basicblock
 *            它的前继集合是header basicblock的Latch basicblock，它的后继是header basicblock
 *            step3.在循环的exit basicblock前插入一个empty basicblock
 *            它的前继是exiting basicblock，它的后继是exit basicblock
 * 
 * 
 */
public class LoopSimplify extends FunctionPass {

    @Override
    public String getName() {
        return "LoopInfoUpdate";
    }

    @Override
    public void runOnFunction(Function func) {
        System.out.println("Running pass : LCSSA");
        LoopInfo loopInfo = func.getLoopInfo();
        for (var topLoop : loopInfo.getTopLevelLoops()) {
            runOnLoop(topLoop);
        }
    }

    private void runOnLoop(Loop topLoop) {
    }
}
