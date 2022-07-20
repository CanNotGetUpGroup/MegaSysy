package backend.machineCode;

import ir.BasicBlock;
import ir.Function;

import util.IList;
import util.IListNode;

public class MachineBasicBlock implements Addressable{
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

    private String label;


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
        this.label = label;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getLabel()).append(":\n");

        var head = instList.getHead();

        while (instList.getLast() != null && head != instList.getLast()) {
            head = head.getNext();
            var i = head.getVal();
            sb.append(i.toString()).append("\n");
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
