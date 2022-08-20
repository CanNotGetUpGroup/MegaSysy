package backend.machineCode;

import backend.machineCode.Instruction.*;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import backend.machineCode.Operand.Shifter;
import ir.instructions.CmpInst;
import util.IListNode;

import java.util.ArrayList;

public abstract class MachineInstruction implements Comparable {
    private MachineBasicBlock parent;
    private IListNode<MachineInstruction, MachineBasicBlock> instNode;
    private MachineInstruction.Ops op;//指令类型

    private static int c = 0;

    private static int counter() {
        return c++;
    }

    public int getId() {
        return id;
    }

    private int id;

    public boolean isSetState() {
        return setState;
    }

    public void setSetState(boolean setState) {
        this.setState = setState;
    }

    private boolean setState;


    public boolean isforBr() {
        return forBr;
    }

    public void setforBr(boolean isforBr) {
        this.forBr = isforBr;
    }

    private boolean forBr;

    public boolean isForFloat() {
        return forFloat;
    }

    public MachineInstruction setForFloat(boolean forFloat, ArrayList<String> typeinfo) {
        this.forFloat = forFloat;
        if (forFloat) this.typeinfo = typeinfo;
        return this;
    }

    public MachineInstruction setForFloat(ArrayList<String> typeinfo) {
        this.forFloat = true;
        this.typeinfo = typeinfo;
        return this;
    }

    public abstract MachineInstruction setForFloat(boolean isForFloat);

    public ArrayList<String> getTypeinfo() {
        return typeinfo;
    }

    public String typeInfoString() {
        if (!isForFloat() || typeinfo == null) return "";
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

    public Shifter getShifter() {
        return shifter;
    }

    public void setShifter(Shifter shifter) {
        this.shifter = shifter;
    }

    public void setShifter(Shift.Type type, int sh) {
        this.shifter = new Shifter(type, sh);
    }

    public boolean hasShift() {
        return shifter != null;
    }

    private Shifter shifter = null;

    private Condition cond;


    public enum Condition {
        EQ, ///> Equal
        NE, ///> Not Equal
        GE, ///> Signed greater than or equal
        LT, ///> Signed less than
        GT, ///> Signed greater than
        LE, ///> Signed less than or equal
        PL; ///> Positive or zero result

        static public Condition getOpposite(Condition cond) {
            return switch (cond) {
                case EQ -> NE;
                case NE -> EQ;
                case GE -> LT;
                case LT -> GE;
                case GT -> LE;
                case LE -> GT;
                default -> throw new RuntimeException("didn't finish" + cond);
            };
        }

        static public Condition irToMCCond(CmpInst.Predicate cond) {
            return switch (cond) {
                // Warn: not translate correctly for float number (ordered, unordered
                case ICMP_EQ, FCMP_UEQ -> MachineInstruction.Condition.EQ;
                case ICMP_NE, FCMP_UNE -> MachineInstruction.Condition.NE;
                case ICMP_SGE, FCMP_UGE -> MachineInstruction.Condition.GE;
                case ICMP_SGT, FCMP_UGT -> MachineInstruction.Condition.GT;
                case ICMP_SLE, FCMP_ULE -> MachineInstruction.Condition.LE;
                case ICMP_SLT, FCMP_ULT -> MachineInstruction.Condition.LT;
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
        Add, Sub, Rsb, Mul, Div, Mod, //Memory
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

    public void insertBefore(IListNode<MachineInstruction, MachineBasicBlock> node) {
        this.getInstNode().insertBefore(node);
    }

    public void insertAfter(MachineInstruction node) {
        this.getInstNode().insertAfter(node.getInstNode());
    }

    public void insertBefore(MachineInstruction node) {
        this.getInstNode().insertBefore(node.getInstNode());
    }

    public void delete() {
        this.getInstNode().remove();
    }

    public MachineInstruction getNext() {
        return this.getInstNode().getNext().getVal();
    }

    public MachineInstruction(MachineBasicBlock parent) {
        this.parent = parent;
        this.id = counter();
        instNode = new IListNode<>(this, parent.getInstList());
    }

    public MachineInstruction(MachineBasicBlock parent, MachineInstruction src) {
        this.parent = parent;
        this.id = counter();
        instNode = new IListNode<>(this, parent.getInstList());
        this.shifter = src.getShifter();
        this.cond = src.getCond();
        this.op = src.getOp();
        this.comment = src.getComment();
        this.typeinfo = src.getTypeinfo() == null ? null : new ArrayList<>(src.getTypeinfo());
        this.setState = src.isSetState();
        this.forFloat = src.isForFloat();
        this.forBr = src.isforBr();
    }


    public Register getDest() {
        return null;
    }

    public void setDest(Register dest) {

    }

    public String condString() {
        if (cond == null)
            return "";
        return cond.toString();
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

    public ArrayList<Register> getDef() {
        var ans = new ArrayList<Register>();
        if (getDest() != null) ans.add(getDest());
        return ans;
    }

    public ArrayList<Register> getUse() {
        var ans = new ArrayList<Register>();

        var op1 = getOp1();
        var op2 = getOp2();
        if (op1 instanceof Register) ans.add((Register) op1);
        if (op2 instanceof Register) {
            ans.add((Register) op2);
        } else if (op2 instanceof Address) {
            var a1 = ((Address) op2).getReg();
            var a2 = ((Address) op2).getOffset();
            if (a1 != null) ans.add(a1);
            if (a2 instanceof Register) ans.add((Register) a2);
        }
        return ans;
    }

    public static MachineInstruction copyMCInstruction(MachineBasicBlock parent, MachineInstruction src) {
        if (src instanceof Arithmetic)
            return new Arithmetic(parent, (Arithmetic) src);
        if (src instanceof Branch)
            return new Branch(parent, (Branch) src);
        if (src instanceof Cmp)
            return new Cmp(parent, (Cmp) src);
        if (src instanceof Comment)
            return new Comment(parent, (Comment) src);
        if (src instanceof LoadImm)
            return new LoadImm(parent, (LoadImm) src);
        if (src instanceof LoadOrStore)
            return new LoadOrStore(parent, (LoadOrStore) src);
        if (src instanceof Move)
            return new Move(parent, (Move) src);
        if (src instanceof PushOrPop)
            return new PushOrPop(parent, (PushOrPop) src);
        if (src instanceof Ubfx)
            return new Ubfx(parent, (Ubfx) src);
        if (src instanceof VCVT)
            return new VCVT(parent, (VCVT) src);
        if (src instanceof VMRS)
            return new VMRS(parent, (VMRS) src);
        if (src instanceof MLAMLS)
            return new MLAMLS(parent, (MLAMLS) src);
        throw new RuntimeException("Copy failed: " + src);
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof MachineInstruction))
            throw new RuntimeException("Can't compare");
        var i = (MachineInstruction) o;
        return i.getId() - getId();
    }

    public static void main(String[] args) {

    }

}
