package backend.machineCode;

import java.util.ArrayList;

/**
 * 全局变量数据块
 */
public class MachineDataBlock implements Addressable {

    public enum Type {
        Float,
        Int,
    }

    private Type type;
    private ArrayList<Integer> integerArrayList;
    private int size;
    private String label;

    public MachineDataBlock(String label, int value) {
        type = Type.Int;
        this.label = label;
        size = 1;
        integerArrayList = new ArrayList<>(value);
    }

    public MachineDataBlock(String label, ArrayList<Integer> arrayList) {
        this.integerArrayList = arrayList;
        this.size = arrayList.size();
        this.type = Type.Int;
        this.label = label;
    }

    public MachineDataBlock(String label, ArrayList<Integer> arrayList, int size) {
        this.integerArrayList = arrayList;
        this.label = label;
        this.size = size;
        this.type = Type.Int;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t.global\t")
                .append(getLabel()).append("\n")
                .append("\t.align\t2\n")
                .append("\t.type\t" + getLabel() + ", %object\n")
                .append("\t.size\t").append(getLabel()).append(",").append(size * 4);
        sb.append(getLabel()).append(":\n");
        if (type == Type.Int) {
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
