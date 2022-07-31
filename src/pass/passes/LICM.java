package pass.passes;

import analysis.LoopInfo;
import ir.Function;
import ir.Loop;
import pass.FunctionPass;

/**
 * 
 * 循环不变量：S是一个语句，已知循环
 * while C
 * do E
 * 当此循环满足: 在任何循环开始前，语句S和C都为真，而且在循环结束后，S仍为真，那么S就是循环不变量。
 * 
 * Algorithm:
 * [concept] https://blog.csdn.net/Truman_Chan/article/details/117702612
 * [travil solution] https://zhuanlan.zhihu.com/p/366794421
 */
public class LICM extends FunctionPass {

    @Override
    public String getName() {
        return "LICM";
    }

    @Override
    public void runOnFunction(Function func) {
        System.out.println("Running pass : LICM");
        LoopInfo loopInfo = func.getLoopInfo();
        for (var topLoop : loopInfo.getTopLevelLoops()) {
            runOnLoop(topLoop);
        }
    }

    public void runOnLoop(Loop loop) {
        for (var subLoop : loop.getSubLoops()) {
            if (subLoop != null) {
                runOnLoop(subLoop);
            }
        }

    }
}
