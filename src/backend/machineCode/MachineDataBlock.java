package backend.machineCode;

import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.MCOperand;
import ir.Constants;
import ir.DerivedTypes;
import ir.GlobalVariable;

import java.util.ArrayList;

import static backend.machineCode.Operand.MCOperand.Type.Addr;

/**
 * 全局变量数据块
 */
public class MachineDataBlock extends MCOperand implements Addressable {

    public enum Type {
        Float,
        Int,
    }

    private Type type;
    private ArrayList<Integer> integerArrayList;

    private GlobalVariable irVal = null;
    private int size;
    private String label;

    public MachineDataBlock(String label, int value) {
        super(Addr);
        type = Type.Int;
        this.label = label;
        size = 1;
        integerArrayList = new ArrayList<>(value);
        integerArrayList.add(value);
    }

    public MachineDataBlock(String label, GlobalVariable g) {
        super(Addr);
        this.label = label;
        this.irVal = g;
        size = ((DerivedTypes.ArrayType) g.getType().getContainedTys(0)).size();
    }

    public MachineDataBlock(String label, ArrayList<Integer> arrayList) {
        super(Addr);
        this.integerArrayList = arrayList;
        this.size = arrayList.size();
        this.type = Type.Int;
        this.label = label;
    }

    public MachineDataBlock(String label, ArrayList<Integer> arrayList, int size) {
        super(Addr);
        this.integerArrayList = arrayList;
        this.label = label;
        this.size = size;
        this.type = Type.Int;
    }

    @Override
    public String getLabel() {
        return label;
    }

    private void printArrContent(StringBuilder sb, DerivedTypes.ArrayType type, Constants.ConstantArray arr) {
        int dim = type.getDim();
        ir.Type kidType = type.getKidType();

        if (arr.isZero()) {
            sb.append("\t.space\t").append(type.size() * 4).append("\n");
        } else if (kidType.isInt32Ty()) {
            for (var i : arr.getArr()) {
                sb.append("\t.word\t").append(((Constants.ConstantInt) i).getVal()).append("\n");
            }
        } else if (kidType.isArrayTy()) {
            for (var i : arr.getArr()) {
                printArrContent(sb, (DerivedTypes.ArrayType) kidType, (Constants.ConstantArray) i);
            }
            if (arr.getArr().size() < type.getNumElements()) {
                sb.append("\t.space\t").append((type.getNumElements() - arr.getArr().size()) * 4 * type.getEleSize()).append("\n");
            }
        } else {
            throw new RuntimeException("Shouldn't come");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t.global\t")
                .append(getLabel()).append("\n")
                .append("\t.align\t2\n")
                .append("\t.type\t" + getLabel() + ", %object\n")
                .append("\t.size\t").append(getLabel()).append(",").append(size * 4).append("\n");
        sb.append(getLabel()).append(":\n");



        if (irVal != null) {
            printArrContent(sb, (DerivedTypes.ArrayType) irVal.getType().getContainedTys(0), (Constants.ConstantArray) irVal.getOperand(0));
        } else if (type == Type.Int) {
            for (var val : integerArrayList) {
                sb.append("\t.word\t").append(val).append("\n");
                if (size > integerArrayList.size())
                    sb.append("\t.space\t").append((size - integerArrayList.size()) * 4).append("\n");
            }
        } else { // type == Float
            throw new RuntimeException("Not finished here");
        }

        return sb.toString();
    }
}
