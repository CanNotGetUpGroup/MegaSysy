package analysis;

import ir.BasicBlock;
import ir.Instruction;
import ir.Type;
import ir.Value;
import util.CloneMap;

import java.util.ArrayList;

/**
 * 官方文档：https://www.llvm.org/docs/MemorySSA.html
 * 介绍视频：https://www.youtube.com/watch?v=bdxWmryoHak
 */
public class MemoryAccess extends Instruction {
    private BasicBlock BB;//不使用Instruction的getParent，因为需要将其插入ilist
    private int ID;

    public MemoryAccess(Ops op, BasicBlock BB) {
        super(Type.getVoidTy(), op);
        this.BB=BB;
    }

    public BasicBlock getBB(){
        return BB;
    }

    public void setBB(BasicBlock BB) {
        this.BB = BB;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    @Override
    public Value copy(CloneMap cloneMap) {
        return null;
    }

    /**
     * Def和Use的基类，对应的指令存在MemoryInstruction，来源memorySSA存在Operand(0)，
     * 使用setDefiningAccess和getDefiningAccess得到
     */
    public static class MemoryDefOrUse extends MemoryAccess {
        private Instruction MemoryInstruction;

        public MemoryDefOrUse(Ops op, BasicBlock BB, Instruction MI, MemoryAccess DMA) {
            super(op, BB);
            MemoryInstruction=MI;
            addOperand(DMA);
        }

        public MemoryDefOrUse(Ops op, Instruction MI, MemoryAccess DMA) {
            super(op, MI.getParent());
            MemoryInstruction=MI;
            addOperand(DMA);
        }

        public void setDefiningAccess(MemoryAccess DMA){
            setOperand(0,DMA);
        }

        public MemoryAccess getDefiningAccess(){
            return (MemoryAccess) getOperand(0);
        }

        public Instruction getMemoryInstruction() {
            return MemoryInstruction;
        }

        public void setMemoryInstruction(Instruction memoryInstruction) {
            MemoryInstruction = memoryInstruction;
        }
    }

    public static class MemoryDef extends MemoryDefOrUse{
        public MemoryDef(Instruction MI, MemoryAccess DMA, int Ver) {
            super(Ops.MemDef,MI,DMA);
            setID(Ver);
        }
    }

    public static class MemoryUse extends MemoryDefOrUse{
        public MemoryUse(Instruction MI, MemoryAccess DMA, int Ver) {
            super(Ops.MemUse,MI,DMA);
            setID(Ver);
        }
    }

    public static class MemoryPhi extends MemoryAccess {
        private final ArrayList<BasicBlock> blocks=new ArrayList<>();

        public MemoryPhi(BasicBlock BB,int Ver) {
            super(Ops.MemPHI,BB);
            setID(Ver);
        }

        public ArrayList<BasicBlock> getBlocks() {
            return blocks;
        }

        public ArrayList<Value> getIncomingValues() {
            return getOperandList();
        }

        public void addIncomingValue(Value V) {
            addOperand(V);
        }

        public void addIncomingBlock(BasicBlock BB) {
            blocks.add(BB);
        }

        public void addIncoming(Value V, BasicBlock BB) {
            addIncomingValue(V);
            addIncomingBlock(BB);
        }

        public BasicBlock getIncomingBlock(int i) {
            if (i > getNumOperands()) return null;
            return blocks.get(i);
        }

        public Value getIncomingValue(int i) {
            if (i > getNumOperands()) return null;
            return getOperand(i);
        }
    }
}
