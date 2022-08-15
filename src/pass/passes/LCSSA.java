package pass.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.BasicBlock;
import ir.Function;
import ir.Instruction;
import ir.Loop;
import ir.Type;
import ir.Use;
import ir.Value;
import ir.Constants.UndefValue;
import ir.Instruction.Ops;
import ir.instructions.Instructions.PHIInst;
import pass.FunctionPass;

/**
 * 在循环退出时跳转到的基本块开头插入冗余 phi 指令，phi 指令 use 循环内定义的值，
 * 循环后面 use 循环内定义的值替换成 use phi，方便循环上的优化
 */
public class LCSSA extends FunctionPass {

    private DominatorTree dominatorTree;

    @Override
    public String getName() {
        return "LCSSA";
    }

    @Override
    public void runOnFunction(Function func) {
        LoopInfo loopInfo = func.getLoopInfo();
        dominatorTree = func.getAndUpdateDominatorTree();
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

        HashSet<Instruction> usedOutLoopList = getUsedOutLoopSet(loop); // 循环里定义，循环外使用的指令
        if (usedOutLoopList.isEmpty()) {
            return;
        }
//        else{
//            System.out.println("[debug] LCSSA......");
//        }

        ArrayList<BasicBlock> exitBlocks = loop.getExitBlocks(); // 循环退出后第一个到达的block
        if (exitBlocks == null || exitBlocks.isEmpty()) {
            System.out.println("exit block is empty so LCSSA failed");
            return;
        }

        for (var inst : usedOutLoopList) {
            generateLoopClosedPhi(inst, loop);
        }
    }

    /**
     * 获取循环里定义，循环外使用的指令set
     * 
     * @param loop
     * @return
     */
    public HashSet<Instruction> getUsedOutLoopSet(Loop loop) {
        HashSet<Instruction> set = new HashSet<>();

        for (var bb : loop.getBbList()) {
            for (var inst : bb.getInstList()) {
                // 遍历inst的use, 如果use是loop外的，则加入set
                for (var use : inst.getUseList()) {
                    var user = use.getU();
                    assert user instanceof Instruction;
                    var userInst = (Instruction) user;
                    var userBB = userInst.getParent();
                    // userInst是phi指令需要特判,userBB取值是phi指令的IncomingBlock
                    if (userInst instanceof PHIInst) {
                        var phi = (PHIInst) userInst;
                        int idx = 0;
                        for (var value : phi.getIncomingValues()) {
                            if (value.getUseList().contains(use)) {
                                userBB = phi.getIncomingBlock(idx);
                            }
                            idx++;
                        }
                    }
                    // the user of inst is out of loop
                    if (!loop.getBbList().contains(userBB) && !set.contains(userBB)) {
                        set.add(inst);
                        break;
                    }
                }
            }
        }
        return set;
    }

    // 删掉 inst 在循环外的 use，用 phi 代替
    public void generateLoopClosedPhi(Instruction inst, Loop loop) {
        var bb = inst.getParent();
        PHIInst phi = null;

        // 在循环出口的基本块开头放置 phi，参数为 inst，即循环内定义的变量 PHI添加到exitBB的最前面
        for (var exitBB : loop.getExitBlocks()) {
            // bb 支配exitBB才放置 phi，否则改变了作用域
            if (dominatorTree.dominates(bb, exitBB)) {
                phi = PHIInst.create(inst.getType(), exitBB.getPredecessorsNum());
                for (int i = 0; i < exitBB.getPredecessors().size(); i++) {
                    phi.addIncoming(inst, exitBB.getPredecessor(i));
                }
                exitBB.getInstList().insertAtHead(phi.getInstNode()); // 插入phi指令到exitBB的最前面
//                System.out.println("[debug] insert phi inst at head of exit block" + phi);
            }
        }

        // 维护 inst指令 在循环外的 user
        ArrayList<Use> usesList = new ArrayList<>(inst.getUseList());
        for (var use : usesList) {
            var userInst = (Instruction) use.getU();
            var userBB = userInst.getParent();
            if (userInst instanceof PHIInst) { // 循环外的use为PHI时，直接跳过
                // var phi = (PHIInst) userInst;
                // int idx = 0;
                // for (var value : phi.getIncomingValues()) {
                // if (value.getUseList().contains(use)) {// userInst是phi指令需要取对应的IncomingBlock
                // userBB = phi.getIncomingBlock(idx);
                // }
                // idx++;
                // }
                continue;
            }
            if (userBB == bb || loop.getBbList().contains(userBB)) { // userBB在循环内无需维护
                continue;
            }
            // 维护循环外的 use
            userInst.COReplaceOperand(inst, phi); // 替换userInst的inst为value
//            System.out.println("[debug] replace " + inst + " with " + phi);
        }
    }

    /**
     * ayame的写的函数，感觉没用，现已废弃
     * inst 在到达 bb 时的值
     */
    public Value getValueForBB(BasicBlock bb, Instruction inst,
            HashMap<BasicBlock, Value> bbToPhiMap, Loop loop) {
        if (bb == null) { // bb为空，返回undef
            return new UndefValue(inst.getType());
        }
        if (bbToPhiMap.get(bb) != null) { // bb已经在bb2phimap里，返回对应的value即可
            return bbToPhiMap.get(bb);
        }
        BasicBlock idom = null;
        for (var pred : bb.getPredecessors()) { // 寻找bb的直接支配节点idmomer var idom = bb.getIdomer();
            if (dominatorTree.dominates(pred, bb)) {
                idom = pred;
                break;
            }
        }
        if (!loop.getBbList().contains(idom)) {
            var value = getValueForBB(idom, inst, bbToPhiMap, loop);
            bbToPhiMap.put(bb, value);
            return value;
        }

        var phi = PHIInst.create(Type.getInt32Ty(), bb.getPredecessors().size());
        bb.getInstList().insertAtHead(phi.getInstNode()); // 将phi指令插入bb的开头
        bbToPhiMap.put(bb, phi);
        for (int i = 0; i < bb.getPredecessors().size(); i++) {
            phi.addOperand(getValueForBB(bb.getPredecessor(i), inst, bbToPhiMap, loop));
        }
        return phi;
    }
}
