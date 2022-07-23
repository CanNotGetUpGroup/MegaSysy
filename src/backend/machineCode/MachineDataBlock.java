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

    private GlobalVariable irVal = null;
    private int size;
    private int singleValue;
    private String label;

    public MachineDataBlock(String label, int value) {
        super(Addr);
        type = Type.Int;
        this.label = label;
        size = 1;
        singleValue = value;
    }

    public MachineDataBlock(String label, GlobalVariable g) {
        super(Addr);
        this.label = label;
        this.irVal = g;
        size = ((DerivedTypes.ArrayType) g.getType().getContainedTys(0)).size();
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
        } else if (kidType.isInt32Ty() || kidType.isFloatTy()) {
            for (var i : arr.getArr()) {
                int value ;
                if(kidType.isInt32Ty()){
                    value = ((Constants.ConstantInt) i).getVal();
                } else {
                    value = Float.floatToIntBits((((Constants.ConstantFP) i).getVal()));
                }
                sb.append("\t.word\t").append(value).append("\n");
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
                .append("\t.align\t2\n").append("\t.type\t").append(getLabel()).append(", %object\n")
                .append("\t.size\t").append(getLabel()).append(", ").append(size * 4).append("\n");
        sb.append(getLabel()).append(":\n");


        if (irVal != null) {
            printArrContent(sb, (DerivedTypes.ArrayType) irVal.getType().getContainedTys(0), (Constants.ConstantArray) irVal.getOperand(0));
        } else
            sb.append("\t.word\t").append(singleValue).append("\n");

        return sb.toString();
    }
}
