package ir.instructions;

import ir.*;
import util.CloneMap;

import static ir.Instruction.Ops;

public class BinaryInstruction extends Instruction {
    public BinaryInstruction(Type type,Ops op, String name, Value S1,Value S2) {
        super(type,op, name, 2);
        addOperand(S1);
        addOperand(S2);
    }

    public BinaryInstruction(Type type, Ops op, Value S1,Value S2) {
        super(type,op, 2);
        addOperand(S1);
        addOperand(S2);
    }

    public BinaryInstruction(Type type, Ops op, Value S1,Value S2,Instruction InsertBefore) {
        super(type,op, 2,InsertBefore);
        addOperand(S1);
        addOperand(S2);
    }

    public static BinaryInstruction create(Ops op,Value S1,Value S2){
        assert S1.getType().equals(S2.getType());
        return new BinaryInstruction(S1.getType(),op,S1,S2);
    }

    public static BinaryInstruction create(Ops op,Value S1,Value S2,Instruction InsertBefore){
        assert S1.getType().equals(S2.getType());
        return new BinaryInstruction(S1.getType(),op,S1,S2,InsertBefore);
    }

    // %14 = xor i1 %13, true
    public static BinaryInstruction createNot(Value Op){
        Constant C= Constants.ConstantInt.const1_1();
        return create(Ops.Xor,Op,C);
    }

    @Override
    public String toString(){
        StringBuilder sb=new StringBuilder();
        sb.append(getName()).append(" = ");
        switch (getOp()) {
            default:
                break;
            case Sub:
                sb.append("sub ");
                break;
            case Add:
                sb.append("add ");
                break;
            case Mul:
                sb.append("mul ");
                break;
            case SDiv:
                sb.append("sdiv ");
                break;
            case SRem:
                sb.append("srem ");
                break;
            case And:
                sb.append("and ");
                break;
            case Or:
                sb.append("or ");
                break;
            case Xor:
                sb.append("xor ");
                break;
            case FSub:
                sb.append("fsub ");
                break;
            case FAdd:
                sb.append("fadd ");
                break;
            case FMul:
                sb.append("fmul ");
                break;
            case FDiv:
                sb.append("fdiv ");
                break;
            case FRem:
                sb.append("frem ");
                break;
        }
        sb.append(getType()).append(" ");
        sb.append(getOperand(0).getName()).append(", ");
        sb.append(getOperand(1).getName());

        return sb.toString();
    }

    @Override
    public BinaryInstruction copy(CloneMap cloneMap) {
        if (cloneMap.get(this) != null) {
            return (BinaryInstruction) cloneMap.get(this);
        }
        BinaryInstruction ret = new BinaryInstruction(getType(),getOp(),getOperand(0).copy(cloneMap),getOperand(1).copy(cloneMap));
        cloneMap.put(this, ret);
        return ret;
    }

    @Override
    public Instruction shallowCopy() {
        return new BinaryInstruction(getType(),getOp(),getOperand(0),getOperand(1));
    }
}
