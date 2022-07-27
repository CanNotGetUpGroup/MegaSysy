package backend.machineCode;

import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import ir.BasicBlock;
import ir.Instruction;
import ir.Type;
import ir.instructions.CmpInst;
import util.IList;
import util.IListNode;
import util.MyIRBuilder;

import java.util.ArrayList;

public abstract class MachineInstruction {
    private MachineBasicBlock parent;
    private IListNode<MachineInstruction, MachineBasicBlock> instNode;
    private MachineInstruction.Ops op;//指令类型

    public boolean isForFloat() {
        return forFloat;
    }

    public MachineInstruction setForFloat(boolean forFloat, ArrayList<String> typeinfo) {
        this.forFloat = forFloat;
        if (forFloat)
            this.typeinfo = typeinfo;
        return this;
    }

    public MachineInstruction setForFloat(ArrayList<String> typeinfo) {
        this.forFloat = true;
        this.typeinfo = typeinfo;
        return this;
    }

    public ArrayList<String> getTypeinfo() {
        return typeinfo;
    }

    public String typeInfoString() {
        if (!isForFloat()) return "";
        StringBuilder sb = new StringBuilder();
        for (var i : typeinfo) {
            sb.append(".").append(i);
        }
        return sb.toString();
    }

    private boolean forFloat = false;
    private ArrayList<String> typeinfo = null;

    private String comment;

    public Condition getCond() {
        return cond;
    }

    public void setCond(Condition cond) {
        this.cond = cond;
    }


    private Condition cond;


    public enum Condition {
        EQ, ///> Equal
        NE, ///> Not Equal
        GE, ///> Signed greater than or equal
        LT, ///> Signed less than
        GT, ///> Signed greater than
        LE; ///> Signed less than or equal

        static public Condition getOpposite(Condition cond) {
            return switch (cond) {
                case EQ -> NE;
                case NE -> EQ;
                case GE -> LT;
                case LT -> GE;
                case GT -> LE;
                case LE -> GT;
            };
        }

        static public Condition irToMCCond(CmpInst.Predicate cond) {
            return switch (cond) {
                // Warn: not translate correctly for float number (ordered, unordered
                case ICMP_EQ, FCMP_OEQ, FCMP_UEQ -> MachineInstruction.Condition.EQ;
                case ICMP_NE, FCMP_ONE, FCMP_UNE -> MachineInstruction.Condition.NE;
                case ICMP_SGE, FCMP_OGE, FCMP_UGE -> MachineInstruction.Condition.GE;
                case ICMP_SGT, FCMP_OGT, FCMP_UGT -> MachineInstruction.Condition.GT;
                case ICMP_SLE, FCMP_OLE, FCMP_ULE -> MachineInstruction.Condition.LE;
                case ICMP_SLT, FCMP_OLT, FCMP_ULT -> MachineInstruction.Condition.LT;
                default -> null;
            };
        }
    }

    public MachineInstruction setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getComment() {
        return comment;
    }

    private boolean isPrologue = false; // 开头

    public boolean isPrologue() {
        return isPrologue;
    }

    public void setPrologue(boolean prologue) {
        isPrologue = prologue;
    }

    public boolean isEpilogue() {
        return isEpilogue;
    }

    public void setEpilogue(boolean epilogue) {
        isEpilogue = epilogue;
    }

    private boolean isEpilogue = false; // 结尾

    public enum Ops {
        //Branch
        BL, B,
        //Unary

        //Binary
        Add, Sub, Rsb, Mul, Div, Mod,
        //Memory
        Ldr, STR, Push, Pop
        //Cast

        //Other

    }

    public MachineBasicBlock getParent() {
        return parent;
    }

    public void setParent(MachineBasicBlock parent) {
        this.parent = parent;
    }

    public IListNode<MachineInstruction, MachineBasicBlock> getInstNode() {
        return instNode;
    }

    public void setInstNode(IListNode<MachineInstruction, MachineBasicBlock> instNode) {
        this.instNode = instNode;
    }

    public MachineInstruction.Ops getOp() {
        return op;
    }

    public void setOp(MachineInstruction.Ops op) {
        this.op = op;
    }

    public void pushBacktoInstList() {
        this.getInstNode().insertIntoListEnd(this.getParent().getInstList());
    }

    public void pushtofront() {
        this.getInstNode().insertBefore(this.getParent().getInstList().getFirst());
    }


    public MachineInstruction(MachineBasicBlock parent) {
        this.parent = parent;
        instNode = new IListNode<>(this, parent.getInstList());
    }


    public Register getDest() {
        return null;
    }

    public void setDest(Register dest) {

    }


    public MCOperand getOp1() {
        return null;
    }

    public void setOp1(MCOperand op) {

    }

    public MCOperand getOp2() {
        return null;
    }

    public void setOp2(MCOperand op) {
    }


    public static void main(String[] args) {

    }

}
