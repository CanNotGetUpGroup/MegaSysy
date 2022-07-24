package ir;

import java.util.ArrayList;

public class Loop {

    private Loop parentLoop; // 父循环
    private BasicBlock loopHeader; // 循环头基本块
    private ArrayList<Loop> subLoops; // loop中的子loop
    private ArrayList<BasicBlock> bbList; // loop中包含的基本块

    private ArrayList<BasicBlock> exitingBlocks; // 循环内即将退出循环的block
    private ArrayList<BasicBlock> exitBlocks; // 循环推出后第一个到达的block
    private ArrayList<BasicBlock> latchBlocks; // 跳转回到头部的基本块

    /**
     * 根据父循环生成loop对象
     * 
     * @param parentLoop 父循环
     */
    public Loop(Loop parentLoop) {
        this.parentLoop = parentLoop;
        this.subLoops = new ArrayList<>();
        this.bbList = new ArrayList<>();
        this.exitingBlocks = new ArrayList<>();
        this.exitBlocks = new ArrayList<>();
        this.latchBlocks = new ArrayList<>();
    }

    /**
     * 根据loop头基本块生成loop对象
     * 
     * @param loopHeader loop头基本块
     */
    public Loop(BasicBlock loopHeader) {
        this.parentLoop = null;
        this.subLoops = new ArrayList<>();
        this.bbList = new ArrayList<>();
        this.exitingBlocks = new ArrayList<>();
        this.exitBlocks = new ArrayList<>();
        this.latchBlocks = new ArrayList<>();
        this.loopHeader = loopHeader;
        this.bbList.add(loopHeader);
    }

    // getter and setter
    public Loop getParentLoop() {
        return parentLoop;
    }

    public void setParentLoop(Loop parentLoop) {
        this.parentLoop = parentLoop;
    }

    public BasicBlock getLoopHeader() {
        return loopHeader;
    }

    public ArrayList<Loop> getSubLoops() {
        return subLoops;
    }

    public ArrayList<BasicBlock> getBbList() {
        return bbList;
    }

    public ArrayList<BasicBlock> getExitingBlocks() {
        return exitingBlocks;
    }

    public ArrayList<BasicBlock> getExitBlocks() {
        return exitBlocks;
    }

    public ArrayList<BasicBlock> getLatchBlocks() {
        return latchBlocks;
    }

    public BasicBlock getSingleLatchBlock() {
        if (getLatchBlocks() == null) {
            return null;
        }
        return getLatchBlocks().get(0);
    }

    // 获取循环结束icmp指令
    public Instruction getLatchCmpInst() {
        if (getSingleLatchBlock() == null) {
            return null;
        }
        return getSingleLatchBlock().getTerminator().getInstNode().getVal();
    }

    public BasicBlock getPreHeader() {
        // ! preHeader 有多个时失效
        BasicBlock preHeader = null;
        int cnt = 0;
        for (var pred : this.loopHeader.getPredecessors()) {
            if (pred.getLoopDepth() != this.loopHeader.getLoopDepth()) {
                preHeader = pred;
                cnt++;
            }
        }
        if (cnt != 1) {
            return null;
        }
        return preHeader;
    }

    public Integer getLoopDepth() {
        int depth = 0;
        for (Loop curLoop = this; curLoop != null; curLoop = curLoop.parentLoop) {
            depth++;
        }
        return depth;
    }

    public BasicBlock getBlockHeader() {
        return bbList.get(0);
    }

    // 1 pre header, 1 latch block, n exit blocks with all pred in loop
    public boolean isCanonical() {
        boolean exitPredInLoop = true;
        for (var exitBB : exitBlocks) {
            for (var pred : exitBB.getPredecessors()) {
                if (!this.getBbList().contains(pred)) {
                    exitPredInLoop = false;
                }
            }
        }
        return latchBlocks.size() == 1 && loopHeader.getPredecessors().size() == 2 && exitPredInLoop;
    }

    // 1 pre header, 1 latch block, 1 exit block
    public boolean isSimpleForLoop() {
        return latchBlocks.size() == 1 && loopHeader.getPredecessors().size() == 2
                && exitBlocks.size() == 1 && exitingBlocks.size() == 1;
    }

    public void addBlock(BasicBlock bb) {
        var loop = this;
        while (loop != null) {
            loop.getBbList().add(bb);
            loop = loop.getParentLoop();
        }
    }

    public void removeBlock(BasicBlock bb) {
        // var loop = this;
        // while(loop != null){
        // loop.getBbList().remove(bb);
        // loop = loop.getParentLoop();
        // }
        this.bbList.remove(bb);
    }

    public void addSubLoop(Loop subLoop) {
        this.subLoops.add(subLoop);
        subLoop.setParentLoop(this);
    }

    public void removeSubLoop(Loop subLoop) {
        this.subLoops.remove(subLoop);
        subLoop.setParentLoop(null);
    }
}
