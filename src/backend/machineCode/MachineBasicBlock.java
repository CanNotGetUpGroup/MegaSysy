package backend.machineCode;

import backend.machineCode.Instruction.Comment;
import ir.BasicBlock;
import ir.Function;

import util.IList;
import util.IListNode;

import java.util.HashSet;

public class MachineBasicBlock implements Addressable {
    private static int c = 1;

    private int counter() {
        return c++;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public HashSet<MachineInstruction> getPredInst() {
        return predInst;
    }

    public void setPredInst(HashSet<MachineInstruction> predInst) {
        this.predInst = predInst;
    }

    public void addPredInst(MachineInstruction inst) {
        this.predInst.add(inst);
    }

    private HashSet<MachineInstruction> predInst = new HashSet<>();

    public HashSet<MachineBasicBlock> getSuccessors() {
        return successors;
    }


    public void addSuccessor(MachineBasicBlock successor) {
        this.successors.add(successor);
    }

    private HashSet<MachineBasicBlock> successors = new HashSet<>();


    private String label;

    private int loopDepth;

    public void setLoopDepth(int l) {
        this.loopDepth = l;
    }

    public int getLoopDepth() {
        return loopDepth;
    }


    private MachineFunction parent;
    private IListNode<MachineBasicBlock, MachineFunction> bbNode;
    private IList<MachineInstruction, MachineBasicBlock> instList;

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

    public MachineBasicBlock(MachineFunction parent) {
        this.parent = parent;
        bbNode = new IListNode<>(this, parent.getBbList());
        instList = new IList<>(this);
        //插入到parent末尾
        bbNode.insertIntoListEnd(this.parent.getBbList());
        label = "." + parent.getLabel() + counter();
    }

    public MachineBasicBlock(MachineFunction parent, String label) {
        this.parent = parent;
        bbNode = new IListNode<>(this, parent.getBbList());
        instList = new IList<>(this);
        //插入到parent末尾
        bbNode.insertIntoListEnd(this.parent.getBbList());
        this.label = "." + label + counter();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getLabel()).append(":\n");

        var head = instList.getHead();

        while (instList.getLast() != null && head != instList.getLast()) {
            head = head.getNext();
            var i = head.getVal();
            if (!(i instanceof Comment))
                sb.append("\t").append(i.toString()).append("\n");
        }
        return sb.toString();
    }

    public MachineFunction getParent() {
        return parent;
    }

    public void setParent(MachineFunction parent) {
        this.parent = parent;
    }

    public IList<MachineInstruction, MachineBasicBlock> getInstList() {
        return instList;
    }

    public void setInstList(IList<MachineInstruction, MachineBasicBlock> instList) {
        this.instList = instList;
    }

    public IListNode<MachineBasicBlock, MachineFunction> getBbNode() {
        return bbNode;
    }

    public void setBbNode(IListNode<MachineBasicBlock, MachineFunction> bbNode) {
        this.bbNode = bbNode;
    }

    public void pushBacktoBBList() {
        this.getBbNode().insertIntoListEnd(this.getParent().getBbList());
    }
}
