package ir;

import ir.instructions.Instructions.*;
import util.IList;
import util.IListNode;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private Function Parent;
    private IListNode<BasicBlock, Function> bbNode;
    private IList<Instruction, BasicBlock> instList;
    private boolean isEntryBlock = false;

    /**
     * 生成基本块对象
     * 
     * @param parent
     * @return
     */
    public static BasicBlock create(Function parent) {
        return new BasicBlock(parent);
    }

    public static BasicBlock create(String name, Function parent) {
        return new BasicBlock(name, parent);
    }

    public BasicBlock(Function parent) {
        super(Type.getLabelTy());
        Parent = parent;
        bbNode = new IListNode<>(this, parent.getBbList());
        instList = new IList<>(this);
        // 插入到parent末尾
        bbNode.insertIntoListEnd(Parent.getBbList());
    }

    public BasicBlock(String name, Function parent) {
        super(Type.getLabelTy(), name);
        Parent = parent;
        bbNode = new IListNode<>(this, parent.getBbList());
        instList = new IList<>(this);
        // 插入到parent末尾
        bbNode.insertIntoListEnd(Parent.getBbList());
    }

    @Override
    public String toString() {
        return getName() + ":";
    }

    public Function getParent() {
        return bbNode.getParent().getVal();
    }

    public void setParent(Function parent) {
        Parent = parent;
    }

    public IList<Instruction, BasicBlock> getInstList() {
        return instList;
    }

    public void setInstList(IList<Instruction, BasicBlock> instList) {
        this.instList = instList;
    }

    public IListNode<BasicBlock, Function> getBbNode() {
        return bbNode;
    }

    public void setBbNode(IListNode<BasicBlock, Function> bbNode) {
        this.bbNode = bbNode;
    }

    // 从函数中删除
    public void remove() {
        bbNode.remove();
        dropUsesAsValue();
        getTerminator().dropUsesAsUser();
    }

    /**
     * 获取终结符指令
     */
    public Instruction getTerminator() {
        return getInstList().getLast().getVal();
    }

    /**
     * 前驱
     */
    public ArrayList<BasicBlock> getPredecessors() {
        ArrayList<BasicBlock> ret = new ArrayList<>();
        for (Use use : getUseList()) {
            if ((use.getU() instanceof Instruction)) {
                ret.add(((Instruction) use.getU()).getParent());
            }
        }
        return ret;
    }

    public BasicBlock getPredecessor(int i) {
        return ((Instruction) getUseList().get(i).getU()).getParent();
    }

    public int getPredecessorsNum() {
        return getUseList().size();
    }

    public void removePredecessor(BasicBlock Pred) {
        if (!(front() instanceof PHIInst)) {
            return;
        }
        int numPred = front().getNumOperands();
        for (Instruction I : getInstList()) {
            if (!(I instanceof PHIInst)) {
                break;
            }
            PHIInst Phi = (PHIInst) I;
            Phi.removeIncomingValue(Pred);
            if (numPred == 1)
                continue;
            Value PhiConstant = Phi.hasConstantValue();
            if (PhiConstant != null) {
                Phi.replaceAllUsesWith(PhiConstant);
                Phi.remove();
            }
        }
    }

    public BasicBlock getOnlyPredecessor() {
        if (getPredecessorsNum() == 0)
            return null;
        BasicBlock B = getPredecessor(0);
        for (int i = 1; i < getPredecessorsNum(); i++) {
            if (getPredecessor(i) != B) {
                return null;
            }
        }
        return B;
    }

    /**
     * 后继
     */
    public ArrayList<BasicBlock> getSuccessors() {
        return getTerminator().getSuccessors();
    }

    public int getSuccessorsNum() {
        return getTerminator().getSuccessorsNum();
    }

    public BasicBlock getSuccessor(int idx) {
        return getTerminator().getSuccessor(idx);
    }

    public void setSuccessor(int idx, BasicBlock BB) {
        getTerminator().setSuccessor(idx, BB);
    }

    public BasicBlock getOnlySuccessor() {
        if (getSuccessorsNum() == 0)
            return null;
        BasicBlock B = getSuccessor(0);
        for (int i = 1; i < getSuccessorsNum(); i++) {
            if (getSuccessor(i) != B) {
                return null;
            }
        }
        return B;
    }

    public boolean isEntryBlock() {
        return isEntryBlock;
    }

    public void setEntryBlock(boolean entryBlock) {
        isEntryBlock = entryBlock;
    }

    /**
     * @return 首条指令
     */
    public Instruction front() {
        return getInstList().getFirst().getVal();
    }

    /**
     *
     * @return 最后一条指令
     */
    public Instruction back() {
        return getInstList().getLast().getVal();
    }

    /**
     * 
     * @return 返回该BB所在loop的深度
     */
    public int getLoopDepth() {
        return this.getParent().getLoopInfo().getLoopDepthForBB(this);
    }
}
