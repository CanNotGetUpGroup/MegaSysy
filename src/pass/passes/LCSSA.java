package pass.passes;

import java.util.ArrayList;
import java.util.HashSet;

import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Loop;
import pass.FunctionPass;

/**
 * 在循环退出时跳转到的基本块开头插入冗余 phi 指令，phi 指令 use 循环内定义的值，
 * 循环后面 use 循环内定义的值替换成 use phi，方便循环上的优化
 */
public class LCSSA extends FunctionPass {

    @Override
    public String getName() {
        return "LCSSA";
    }

    @Override
    public void runOnFunction(Function func) {
        System.out.println("Running pass : LCSSA");
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

        ArrayList<Instruction> usedOutLoopList = getUsedOutLoopList(loop); // 循环里定义，循环外使用的指令
        if (usedOutLoopList.isEmpty()) {
            return;
        }

        ArrayList<BasicBlock> exitBlocks = loop.getExitBlocks(); // 循环退出后第一个到达的block
        if (exitBlocks == null || exitBlocks.isEmpty()) {
            return;
        }

        for (var inst : usedOutLoopList) {
            generateLoopClosedPhi(inst, loop);
        }
    }

    public ArrayList<Instruction> getUsedOutLoopList(Loop loop) {
        ArrayList<Instruction> usedOutLoopList = new ArrayList<>();

        return usedOutLoopList;
    }

    public void generateLoopClosedPhi(Instruction inst, Loop loop) {
    }
}
